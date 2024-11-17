package org.tinystruct.data.repository;

import org.tinystruct.data.Repository;

/**
 * Enum representing different types of database.
 */
public enum Type {
    MySQL, SQLServer, SQLite, Redis, H2;

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
            default:
                repository = new MySQLServer();
                break;
        }

        return repository;
    }
}
