package nl.gmt.data;

public class DbConfiguration {
    private String connectionString;
    private DbType type;
    private RepositoryService repositoryService;
    private boolean enableMultiTenancy;
    private OnResolveMessage messageResolver;
    private int connectionPoolMinSize = -1;
    private int connectionPoolMaxSize = -1;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public DbType getType() {
        return type;
    }

    public void setType(DbType type) {
        this.type = type;
    }

    public boolean isEnableMultiTenancy() {
        return enableMultiTenancy;
    }

    public void setEnableMultiTenancy(boolean enableMultiTenancy) {
        this.enableMultiTenancy = enableMultiTenancy;
    }

    public OnResolveMessage getMessageResolver() {
        return messageResolver;
    }

    public void setMessageResolver(OnResolveMessage messageResolver) {
        this.messageResolver = messageResolver;
    }

    public interface OnResolveMessage {
        String onResolveMessage(String key);
    }

    public int getConnectionPoolMinSize() {
        return connectionPoolMinSize;
    }

    public void setConnectionPoolMinSize(int connectionPoolMinSize) {
        this.connectionPoolMinSize = connectionPoolMinSize;
    }

    public int getConnectionPoolMaxSize() {
        return connectionPoolMaxSize;
    }

    public void setConnectionPoolMaxSize(int connectionPoolMaxSize) {
        this.connectionPoolMaxSize = connectionPoolMaxSize;
    }
}
