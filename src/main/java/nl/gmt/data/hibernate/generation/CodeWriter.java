package nl.gmt.data.hibernate.generation;

import org.apache.commons.lang.StringUtils;

class CodeWriter {
    private int indent;
    private StringBuilder sb = new StringBuilder();

    public void indent() {
        indent++;
    }

    public void unIndent() {
        indent--;
    }

    public void writeln() {
        writeln(null);
    }

    public void writeln(String format, Object... args) {
        if (StringUtils.isEmpty(format)) {
            sb.append("\n");
            return;
        }

        if (indent > 0)
            sb.append(StringUtils.repeat(" ", indent * 4));

        if (args != null && args.length > 0)
            format = String.format(format, args);

        sb.append(format);
        sb.append("\n");
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
