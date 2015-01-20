package nl.gmt.data;

public class DbConfiguration {
    private String connectionString;
    private DbType type;
    private RepositoryService repositoryService;
    private boolean enableMultiTenancy;

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
}
