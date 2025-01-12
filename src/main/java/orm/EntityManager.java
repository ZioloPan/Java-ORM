package orm;

import orm.annotations.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Klasa EntityManager zarządzająca operacjami CRUD na encjach.
 */
public class EntityManager {

    private final ConnectionPool connectionPool;

    public EntityManager(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    /**
     * Zapisuje nową encję w bazie danych.
     *
     * @param entity obiekt do zapisania
     */
    public <T> void save(T entity) {
        Class<?> clazz = entity.getClass();

        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("Class " + clazz.getName() + " is not mapped in DB");
        }

        String tableName = table.name();
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();

        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);

                if (field.isAnnotationPresent(Id.class)) {
                    continue;
                }

                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    columns.append(column.name()).append(",");
                    values.append("'").append(field.get(entity)).append("',");
                }

                if (field.isAnnotationPresent(OneToOne.class)) {
                    OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                    Object relatedEntity = field.get(entity);

                    if (relatedEntity != null) {
                        Field relatedIdField = Arrays.stream(relatedEntity.getClass().getDeclaredFields())
                                .filter(f -> f.isAnnotationPresent(Id.class))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Related entity " + relatedEntity.getClass().getName() + " must have @Id"));

                        relatedIdField.setAccessible(true);
                        Object relatedIdValue = relatedIdField.get(relatedEntity);

                        if (relatedIdValue == null) {
                            save(relatedEntity);
                            relatedIdValue = relatedIdField.get(relatedEntity);
                        }

                        columns.append(oneToOne.column()).append(",");
                        values.append("'").append(relatedIdValue).append("',");
                    }
                }
            }

            String columnsString = columns.substring(0, columns.length() - 1);
            String valuesString = values.substring(0, values.length() - 1);

            String query = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnsString, valuesString);

            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Field idField = Arrays.stream(clazz.getDeclaredFields())
                                .filter(f -> f.isAnnotationPresent(Id.class))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Entity " + clazz.getName() + " must have @Id"));

                        idField.setAccessible(true);
                        idField.set(entity, generatedKeys.getObject(1));
                    }
                }
                connectionPool.notifyObservers("Sukces: Encja zapisana w tabeli " + tableName);
            }

        } catch (Exception e) {
            connectionPool.notifyObservers("Błąd: Klasa " + clazz.getName() + " nie jest oznaczona jako @Table");
            throw new RuntimeException("Entity save Error: " + e.getMessage());
        }
    }




    /**
     * Wyszukuje encję w bazie danych po identyfikatorze.
     *
     * @param clazz klasa encji
     * @param id    identyfikator
     * @param <T>   typ encji
     * @return encja lub null, jeśli nie znaleziono
     */
    public <T> T find(Class<T> clazz, Object id) {
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            connectionPool.notifyObservers("Błąd: Klasa " + clazz.getName() + " nie jest oznaczona jako @Table");
            throw new RuntimeException("Klasa " + clazz.getName() + " nie jest oznaczona jako @Table");
        }

        String tableName = table.name();
        String idColumn = null;
        Object idValue = id;

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    idColumn = column.name();
                } else {
                    idColumn = field.getName();
                }
                break;
            }
        }

        if (idColumn == null) {
            connectionPool.notifyObservers("Błąd: Klasa " + clazz.getName() + " nie zawiera pola oznaczonego jako @Id");
            throw new RuntimeException("Klasa " + clazz.getName() + " nie zawiera pola oznaczonego jako @Id");
        }

        String query = String.format("SELECT * FROM %s WHERE %s = ?", tableName, idColumn);

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setObject(1, idValue);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                T entity = clazz.getDeclaredConstructor().newInstance();

                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);

                    if (field.isAnnotationPresent(Column.class)) {
                        Column column = field.getAnnotation(Column.class);
                        field.set(entity, resultSet.getObject(column.name()));
                    }

                    if (field.isAnnotationPresent(ManyToOne.class)) {
                        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                        String foreignKeyColumn = manyToOne.column() + "_id";

                        Object foreignKeyValue = resultSet.getObject(foreignKeyColumn);
                        if (foreignKeyValue != null) {
                            Class<?> relatedClass = field.getType();
                            Object relatedEntity = find(relatedClass, foreignKeyValue);
                            field.set(entity, relatedEntity);
                        }
                    }

                    if (field.isAnnotationPresent(OneToOne.class)) {
                        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                        String foreignKeyColumn = oneToOne.column();


                        Object foreignKeyValue = resultSet.getObject(foreignKeyColumn);
                        if (foreignKeyValue != null) {
                            Class<?> relatedClass = field.getType();
                            Object relatedEntity = find(relatedClass, foreignKeyValue);
                            field.set(entity, relatedEntity);
                        }
                    }

                    if (field.isAnnotationPresent(OneToMany.class)) {
                        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                        String mappedBy = oneToMany.mappedBy();

                        Class<?> elementType = (Class<?>) ((java.lang.reflect.ParameterizedType) field.getGenericType())
                                .getActualTypeArguments()[0];

                        String relatedTableName = elementType.getAnnotation(Table.class).name();
                        String relatedQuery = String.format("SELECT * FROM %s WHERE %s = ?", relatedTableName, mappedBy + "_id");

                        try (PreparedStatement relatedStmt = connection.prepareStatement(relatedQuery)) {
                            relatedStmt.setObject(1, idValue);
                            ResultSet relatedResultSet = relatedStmt.executeQuery();

                            List<Object> relatedEntities = new ArrayList<>();
                            while (relatedResultSet.next()) {
                                Object relatedEntity = elementType.getDeclaredConstructor().newInstance();
                                for (Field relatedField : elementType.getDeclaredFields()) {
                                    Column column = relatedField.getAnnotation(Column.class);
                                    if (column != null) {
                                        relatedField.setAccessible(true);
                                        relatedField.set(relatedEntity, relatedResultSet.getObject(column.name()));
                                    }
                                }
                                relatedEntities.add(relatedEntity);
                            }

                            field.set(entity, relatedEntities);
                        }
                    }

                    if (field.isAnnotationPresent(ManyToMany.class)) {
                        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
                        String joinTable = manyToMany.joinTable();
                        String joinColumn = manyToMany.joinColumn();
                        String inverseJoinColumn = manyToMany.inverseJoinColumn();

                        Class<?> relatedClass = (Class<?>) ((java.lang.reflect.ParameterizedType) field.getGenericType())
                                .getActualTypeArguments()[0];

                        String relatedTableName = relatedClass.getAnnotation(Table.class).name();
                        String joinQuery = String.format(
                                "SELECT r.* FROM %s jt INNER JOIN %s r ON jt.%s = r.id WHERE jt.%s = ?",
                                joinTable, relatedTableName, inverseJoinColumn, joinColumn
                        );

                        try (PreparedStatement joinStmt = connection.prepareStatement(joinQuery)) {
                            joinStmt.setObject(1, idValue);
                            ResultSet relatedResultSet = joinStmt.executeQuery();

                            List<Object> relatedEntities = new ArrayList<>();
                            while (relatedResultSet.next()) {
                                Object relatedEntity = relatedClass.getDeclaredConstructor().newInstance();
                                for (Field relatedField : relatedClass.getDeclaredFields()) {
                                    Column column = relatedField.getAnnotation(Column.class);
                                    if (column != null) {
                                        relatedField.setAccessible(true);
                                        relatedField.set(relatedEntity, relatedResultSet.getObject(column.name()));
                                    }
                                }
                                relatedEntities.add(relatedEntity);
                            }

                            field.set(entity, relatedEntities);
                        }
                    }
                }
                connectionPool.notifyObservers("Sukces: Znaleziono encję w tabeli " + tableName + " o ID " + id);
                return entity;
            } else {
                connectionPool.notifyObservers("Nie znaleziono encji w tabeli " + tableName + " o ID " + id);
            }

        } catch (Exception e) {
            connectionPool.notifyObservers("Błąd podczas wyszukiwania encji: " + e.getMessage());
            throw new RuntimeException("Błąd podczas wyszukiwania encji: " + e.getMessage());
        }

        return null;
    }

    /**
     * Aktualizuje istniejącą encję w bazie danych.
     *
     * @param entity encja do aktualizacji
     */
    public <T> void update(T entity) {
        Class<?> clazz = entity.getClass();
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            connectionPool.notifyObservers("Błąd: Klasa " + clazz.getName() + " nie jest oznaczona jako @Table");
            throw new RuntimeException("Klasa " + clazz.getName() + " nie jest oznaczona jako @Table");
        }

        String tableName = table.name();
        StringBuilder setClause = new StringBuilder();
        String idColumn = null;
        Object idValue = null;

        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    if (field.isAnnotationPresent(Id.class)) {
                        idColumn = column.name();
                        idValue = field.get(entity);
                    } else {
                        setClause.append(column.name()).append(" = '").append(field.get(entity)).append("',");
                    }
                }
            }

            if (idColumn == null || idValue == null) {
                connectionPool.notifyObservers("Błąd: Encja " + clazz.getName() + " nie zawiera poprawnego klucza głównego");
                throw new RuntimeException("Encja " + clazz.getName() + " nie zawiera poprawnego klucza głównego");
            }

            String query = String.format("UPDATE %s SET %s WHERE %s = ?",
                    tableName,
                    setClause.substring(0, setClause.length() - 1),
                    idColumn);

            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setObject(1, idValue);
                statement.executeUpdate();
            }
            connectionPool.notifyObservers("Sukces: Zaktualizowano encję w tabeli " + tableName);
        } catch (Exception e) {
            connectionPool.notifyObservers("Błąd podczas aktualizacji encji: " + e.getMessage());
            throw new RuntimeException("Błąd podczas aktualizacji encji: " + e.getMessage());
        }
    }

    /**
     * Usuwa encję z bazy danych.
     *
     * @param entity encja do usunięcia
     */
    public <T> void delete(T entity) {
        Class<?> clazz = entity.getClass();
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("Klasa " + clazz.getName() + " nie jest oznaczona jako @Table");
        }

        String tableName = table.name();
        String idColumn = null;
        Object idValue = null;

        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    Column column = field.getAnnotation(Column.class);
                    idColumn = column.name();
                    idValue = field.get(entity);
                    break;
                }
            }

            if (idColumn == null || idValue == null) {
                throw new RuntimeException("Encja " + clazz.getName() + " nie zawiera poprawnego klucza głównego");
            }

            String query = String.format("DELETE FROM %s WHERE %s = ?", tableName, idColumn);

            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setObject(1, idValue);
                statement.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Błąd podczas usuwania encji: " + e.getMessage());
        }
    }
}
