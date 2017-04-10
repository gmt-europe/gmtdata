package nl.gmt.data.migrate.sqlserver;

class SqlUtil {
    public static String escape(String value) {
        if (value == null)
            return "NULL";

        StringBuilder sb = new StringBuilder();

        sb.append('\'');

        boolean hadControl = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c))
            {
                if (!hadControl)
                    sb.append("'");

                sb.append(" || CHR(").append((int)c).append(")");
                hadControl = true;
            }
            else
            {
                if (hadControl)
                {
                    sb.append(" || '");
                    hadControl = false;
                }

                if (c == '\'')
                    sb.append(c);

                sb.append(c);
            }
        }

        sb.append('\'');

        return sb.toString();
    }
}
