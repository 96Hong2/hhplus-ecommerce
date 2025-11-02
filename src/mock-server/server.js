const jsonServer = require('json-server');
const server = jsonServer.create();
const router = jsonServer.router('db.json');
const middlewares = jsonServer.defaults();

// 미들웨어 설정
server.use(middlewares);
server.use(jsonServer.bodyParser);

// ========================================
// 커스텀 엔드포인트 (비즈니스 로직 시뮬레이션)
// ========================================

/**
 * 쿠폰 선착순 발급
 * PATCH /api/coupons/:couponId/issue
 */
server.patch('/api/coupons/:couponId/issue', (req, res) => {
  const db = router.db;
  const couponId = parseInt(req.params.couponId);
  const { userId } = req.body;

  const coupon = db.get('coupons').find({ id: String(couponId) }).value();

  if (!coupon) {
    return res.status(404).json({
      success: false,
      data: null,
      message: '쿠폰을 찾을 수 없습니다',
      errorCode: 'COUPON_NOT_FOUND'
    });
  }

  // 발급 수량 체크
  if (coupon.issuedCount >= coupon.maxIssueCount) {
    return res.status(409).json({
      success: false,
      data: null,
      message: '쿠폰이 모두 소진되었습니다',
      errorCode: 'COUPON_EXHAUSTED'
    });
  }

  // 중복 발급 체크
  const existingUserCoupon = db.get('userCoupons')
    .find({ userId: parseInt(userId), couponId: couponId })
    .value();

  if (existingUserCoupon) {
    return res.status(400).json({
      success: false,
      data: null,
      message: '이미 발급받은 쿠폰입니다',
      errorCode: 'COUPON_ALREADY_ISSUED'
    });
  }

  // 쿠폰 발급
  const newUserCoupon = {
    id: String(Date.now()),
    userId: parseInt(userId),
    couponId: couponId,
    isUsed: false,
    usedAt: null,
    orderId: null,
    issuedAt: new Date().toISOString()
  };

  db.get('userCoupons').push(newUserCoupon).write();

  // 발급 수량 증가
  db.get('coupons')
    .find({ id: String(couponId) })
    .assign({
      issuedCount: coupon.issuedCount + 1,
      updatedAt: new Date().toISOString()
    })
    .write();

  res.status(201).json({
    success: true,
    data: {
      ...newUserCoupon,
      couponName: coupon.couponName,
      discountType: coupon.discountType,
      discountValue: coupon.discountValue
    },
    message: '쿠폰이 발급되었습니다'
  });
});

/**
 * 포인트 충전
 * POST /api/point/charge/:userId
 */
server.post('/api/point/charge/:userId', (req, res) => {
  const db = router.db;
  const userId = parseInt(req.params.userId);
  const { amount, description } = req.body;

  const user = db.get('users').find({ id: String(userId) }).value();

  if (!user) {
    return res.status(404).json({
      success: false,
      data: null,
      message: '사용자를 찾을 수 없습니다',
      errorCode: 'USER_NOT_FOUND'
    });
  }

  // 최소 충전 금액 검증
  if (amount < 1000) {
    return res.status(400).json({
      success: false,
      data: null,
      message: '최소 충전 금액은 1,000원입니다',
      errorCode: 'INVALID_CHARGE_AMOUNT'
    });
  }

  // 1000원 단위 검증
  if (amount % 1000 !== 0) {
    return res.status(400).json({
      success: false,
      data: null,
      message: '충전 금액은 1,000원 단위여야 합니다',
      errorCode: 'INVALID_CHARGE_UNIT'
    });
  }

  const newBalance = user.pointBalance + amount;

  // 포인트 잔액 업데이트
  db.get('users')
    .find({ id: String(userId) })
    .assign({
      pointBalance: newBalance,
      updatedAt: new Date().toISOString()
    })
    .write();

  // 포인트 히스토리 추가
  const pointHistory = {
    id: String(Date.now()),
    userId: userId,
    transactionType: 'CHARGE',
    amount: amount,
    balanceAfter: newBalance,
    orderId: null,
    description: description || '포인트 충전',
    createdAt: new Date().toISOString()
  };

  db.get('pointHistories').push(pointHistory).write();

  res.status(200).json({
    success: true,
    data: pointHistory,
    message: '포인트가 충전되었습니다'
  });
});

/**
 * 주문 생성 (재고 차감 포함)
 * POST /api/order/:userId
 */
server.post('/api/order/:userId', (req, res) => {
  const db = router.db;
  const userId = parseInt(req.params.userId);
  const { items, couponId, usedPoints } = req.body;

  const user = db.get('users').find({ id: String(userId) }).value();

  if (!user) {
    return res.status(404).json({
      success: false,
      data: null,
      message: '사용자를 찾을 수 없습니다',
      errorCode: 'USER_NOT_FOUND'
    });
  }

  // 재고 확인
  for (const item of items) {
    const productOption = db.get('productOptions')
      .find({ id: String(item.productOptionId) })
      .value();

    if (!productOption) {
      return res.status(404).json({
        success: false,
        data: null,
        message: '상품 옵션을 찾을 수 없습니다',
        errorCode: 'PRODUCT_OPTION_NOT_FOUND'
      });
    }

    if (productOption.stockQuantity < item.quantity) {
      return res.status(400).json({
        success: false,
        data: null,
        message: `재고가 부족합니다 (${productOption.optionName})`,
        errorCode: 'STOCK_INSUFFICIENT'
      });
    }
  }

  // 주문 금액 계산
  let totalAmount = 0;
  const orderItemsData = [];

  for (const item of items) {
    const productOption = db.get('productOptions')
      .find({ id: String(item.productOptionId) })
      .value();

    const product = db.get('products')
      .find({ id: String(productOption.productId) })
      .value();

    const subtotal = productOption.optionPrice * item.quantity;
    totalAmount += subtotal;

    orderItemsData.push({
      productId: productOption.productId,
      productOptionId: item.productOptionId,
      productName: product.productName,
      optionName: productOption.optionName,
      productPrice: productOption.optionPrice,
      quantity: item.quantity,
      subtotal: subtotal
    });
  }

  // 쿠폰 할인 계산
  let discountAmount = 0;
  if (couponId) {
    const userCoupon = db.get('userCoupons')
      .find({ userId: userId, couponId: parseInt(couponId), isUsed: false })
      .value();

    if (userCoupon) {
      const coupon = db.get('coupons').find({ id: String(couponId) }).value();

      if (coupon.discountType === 'FIXED') {
        discountAmount = coupon.discountValue;
      } else if (coupon.discountType === 'PERCENTAGE') {
        discountAmount = Math.floor(totalAmount * (coupon.discountValue / 100));
      }
    }
  }

  const finalAmount = totalAmount - discountAmount - (usedPoints || 0);

  // 주문 생성
  const orderNumber = `ORD-${new Date().toISOString().split('T')[0].replace(/-/g, '')}-${String(Date.now()).slice(-4)}`;
  const order = {
    id: String(Date.now()),
    orderNumber: orderNumber,
    userId: userId,
    totalAmount: totalAmount,
    discountAmount: discountAmount,
    finalAmount: finalAmount,
    usedPoints: usedPoints || 0,
    couponId: couponId ? parseInt(couponId) : null,
    orderStatus: 'PENDING',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  db.get('orders').push(order).write();

  // 주문 아이템 생성 및 재고 차감
  for (let i = 0; i < orderItemsData.length; i++) {
    const itemData = orderItemsData[i];
    const item = items[i];

    const orderItem = {
      id: String(Date.now() + i),
      orderId: parseInt(order.id),
      ...itemData,
      itemStatus: 'PREPARING',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    db.get('orderItems').push(orderItem).write();

    // 재고 차감
    const productOption = db.get('productOptions')
      .find({ id: String(item.productOptionId) })
      .value();

    const newStockQuantity = productOption.stockQuantity - item.quantity;

    db.get('productOptions')
      .find({ id: String(item.productOptionId) })
      .assign({
        stockQuantity: newStockQuantity,
        soldOutFlag: newStockQuantity === 0,
        updatedAt: new Date().toISOString()
      })
      .write();
  }

  res.status(201).json({
    success: true,
    data: order,
    message: '주문이 생성되었습니다'
  });
});

/**
 * 결제 처리 (포인트 차감, 쿠폰 사용 처리)
 * POST /api/order/:orderId/payment
 */
server.post('/api/order/:orderId/payment', (req, res) => {
  const db = router.db;
  const orderId = parseInt(req.params.orderId);

  const order = db.get('orders').find({ id: String(orderId) }).value();

  if (!order) {
    return res.status(404).json({
      success: false,
      data: null,
      message: '주문을 찾을 수 없습니다',
      errorCode: 'ORDER_NOT_FOUND'
    });
  }

  if (order.orderStatus !== 'PENDING') {
    return res.status(400).json({
      success: false,
      data: null,
      message: '결제할 수 없는 주문 상태입니다',
      errorCode: 'INVALID_ORDER_STATUS'
    });
  }

  const user = db.get('users').find({ id: String(order.userId) }).value();

  // 포인트 잔액 확인
  if (order.usedPoints > 0 && user.pointBalance < order.usedPoints) {
    return res.status(400).json({
      success: false,
      data: null,
      message: '포인트 잔액이 부족합니다',
      errorCode: 'POINT_INSUFFICIENT'
    });
  }

  // 포인트 차감
  if (order.usedPoints > 0) {
    const newBalance = user.pointBalance - order.usedPoints;

    db.get('users')
      .find({ id: String(order.userId) })
      .assign({
        pointBalance: newBalance,
        updatedAt: new Date().toISOString()
      })
      .write();

    // 포인트 히스토리 추가
    const pointHistory = {
      id: String(Date.now()),
      userId: order.userId,
      transactionType: 'USE',
      amount: order.usedPoints,
      balanceAfter: newBalance,
      orderId: orderId,
      description: '주문 결제',
      createdAt: new Date().toISOString()
    };

    db.get('pointHistories').push(pointHistory).write();
  }

  // 쿠폰 사용 처리
  if (order.couponId) {
    db.get('userCoupons')
      .find({ userId: order.userId, couponId: order.couponId })
      .assign({
        isUsed: true,
        usedAt: new Date().toISOString(),
        orderId: orderId
      })
      .write();
  }

  // 주문 상태 변경
  db.get('orders')
    .find({ id: String(orderId) })
    .assign({
      orderStatus: 'PAID',
      updatedAt: new Date().toISOString()
    })
    .write();

  res.status(200).json({
    success: true,
    data: {
      orderId: orderId,
      orderNumber: order.orderNumber,
      paymentStatus: 'PAID',
      finalAmount: order.finalAmount,
      paidAt: new Date().toISOString()
    },
    message: '결제가 완료되었습니다'
  });
});

/**
 * 장바구니 추가 (중복 시 수량 합산)
 * POST /api/cart/:userId
 */
server.post('/api/cart/:userId', (req, res) => {
  const db = router.db;
  const userId = parseInt(req.params.userId);
  const { productOptionId, quantity } = req.body;

  // 기존 장바구니 항목 확인
  const existingCart = db.get('carts')
    .find({ userId: userId, productOptionId: parseInt(productOptionId) })
    .value();

  if (existingCart) {
    // 수량 합산
    const newQuantity = existingCart.quantity + quantity;

    db.get('carts')
      .find({ id: existingCart.id })
      .assign({
        quantity: newQuantity,
        updatedAt: new Date().toISOString()
      })
      .write();

    const updatedCart = db.get('carts').find({ id: existingCart.id }).value();

    return res.status(200).json({
      success: true,
      data: updatedCart,
      message: '장바구니 수량이 업데이트되었습니다'
    });
  }

  // 새 장바구니 항목 추가
  const newCart = {
    id: String(Date.now()),
    userId: userId,
    productOptionId: parseInt(productOptionId),
    quantity: quantity,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  db.get('carts').push(newCart).write();

  res.status(201).json({
    success: true,
    data: newCart,
    message: '장바구니에 추가되었습니다'
  });
});

// ========================================
// 기본 JSON Server 라우터 사용
// ========================================
server.use('/api', router);

// 서버 시작
const PORT = 3001;
server.listen(PORT, () => {
  console.log('🚀 Mock API Server is running!');
  console.log(`📍 URL: http://localhost:${PORT}`);
  console.log('📖 API Documentation: http://localhost:' + PORT);
  console.log('\n✨ Custom Endpoints:');
  console.log('   - PATCH /api/coupons/:couponId/issue');
  console.log('   - POST  /api/point/charge/:userId');
  console.log('   - POST  /api/order/:userId');
  console.log('   - POST  /api/order/:orderId/payment');
  console.log('   - POST  /api/cart/:userId');
  console.log('\n📦 Resources:');
  console.log('   - /api/products, /api/productOptions');
  console.log('   - /api/users, /api/carts');
  console.log('   - /api/orders, /api/orderItems');
  console.log('   - /api/coupons, /api/userCoupons');
  console.log('   - /api/pointHistories, /api/integrationLogs');
});
