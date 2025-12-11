package hhplus.ecommerce.common.config.converter;

import hhplus.ecommerce.product.domain.model.PeriodType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PeriodTypeConverter implements Converter<String, PeriodType> {

    @Override
    public PeriodType convert(@Nullable String source) {
        if (source == null) return null;
        String normalized = source.trim();
        if (normalized.isEmpty()) return null;

        String upper = normalized.toUpperCase().replace('-', '_');
        try {
            return PeriodType.valueOf(upper);
        } catch (IllegalArgumentException ignored) {
            // continue
        }

        for (PeriodType type : PeriodType.values()) {
            if (type.getDescription().equalsIgnoreCase(normalized)) {
                return type;
            }
        }

        switch (upper) {
            case "DAY":
            case "DAILY" :
                return PeriodType.DAILY;
            case "WEEK":
            case "WEEKLY":
                return PeriodType.WEEKLY;
            case "MONTH":
            case "MONTHLY":
                return PeriodType.MONTHLY;
            case "ALL":
            case "ALLTIME":
            case "ALL_TIME":
                return PeriodType.ALL_TIME;
            default:
                throw new IllegalArgumentException("Unknown PeriodType: " + source);
        }
    }
}
