package org.tinystruct.data.repository;

import org.tinystruct.data.Repository;

/**
 * Enum representing different types of database.
 */
public enum Type {
    MySQL, SQLServer, SQLite, H2, Redis;

    public Repository createRepository() {
        Repository repository;
        switch (this.ordinal()) {
            case 1:
                repository = new SQLServer();
                break;
            case 2:
                repository = new SQLiteServer();
                break;
            case 3:
                repository = new H2Server();
                break;
            case 4:
                repository = new RedisServer();
                break;
            default:
                repository = new MySQLServer();
                break;
        }

        return repository;
    }
}
