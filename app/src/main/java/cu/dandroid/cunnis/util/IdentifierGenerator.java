package cu.dandroid.cunnis.util;

import cu.dandroid.cunnis.data.local.db.dao.RabbitDao;

public final class IdentifierGenerator {
    private IdentifierGenerator() {}

    public static String generateNext(RabbitDao rabbitDao) {
        Integer maxId = rabbitDao.getMaxNumericIdentifier();
        int next = (maxId != null ? maxId + 1 : 1);
        if (next > Constants.MAX_IDENTIFIER) {
            // Fallback: if we exceed 999999, wrap around or use alphanumeric
            return String.valueOf(next % (int) Constants.MAX_IDENTIFIER);
        }
        return String.format("%0" + Constants.MAX_ID_LENGTH + "d", next);
    }
}
