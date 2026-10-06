package at.technikum.dms.mapper;

final class MapperUtils {

    private MapperUtils() {
    }

    static String trim(String value) {
        return value == null ? null : value.strip();
    }

    static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}

