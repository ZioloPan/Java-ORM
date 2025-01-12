package orm;

import orm.annotations.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

                handleColumnField(field, entity, columns, values);
                handleOneToOneField(field, entity, columns, values);
                handleManyToOneField(field, entity, columns, values);
                handleOneToManyField(field, entity);
                handleManyToManyField(field, entity);
            }

            String columnsString = columns.substring(0, columns.length() - 1);
            String valuesString = values.substring(0, values.length() - 1);

            String query = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnsString, valuesString);

            executeInsertQuery(query, clazz, entity, tableName);

        } catch (Exception e) {
            throw new RuntimeException("Entity save Error: " + e.getMessage());
        }
    }


    private void handleColumnField(Field field, Object entity, StringBuilder columns, StringBuilder values) throws IllegalAccessException {
        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            columns.append(column.name()).append(",");
            values.append("'").append(field.get(entity)).append("',");
        }
    }

    private void handleOneToOneField(Field field, Object entity, StringBuilder columns, StringBuilder values) throws IllegalAccessException {
        if (field.isAnnotationPresent(OneToOne.class)) {
            OneToOne oneToOne = field.getAnnotation(OneToOne.class);
            Object relatedEntity = field.get(entity);

            if (relatedEntity != null) {
                Field relatedIdField = getIdField(relatedEntity.getClass());
                relatedIdField.setAccessible(true);

                Object relatedIdValue = relatedIdField.get(relatedEntity);

                columns.append(oneToOne.column()).append(",");
                values.append("'").append(relatedIdValue).append("',");
            }
        }
    }

    private void handleManyToOneField(Field field, Object entity, StringBuilder columns, StringBuilder values) throws IllegalAccessException {
        if (field.isAnnotationPresent(ManyToOne.class)) {
            ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
            Object relatedEntity = field.get(entity);

            if (relatedEntity != null) {
                Field relatedIdField = getIdField(relatedEntity.getClass());
                relatedIdField.setAccessible(true);

                Object relatedIdValue = relatedIdField.get(relatedEntity);

                columns.append(manyToOne.column()).append(",");
                values.append("'").append(relatedIdValue).append("',");
            }
        }
    }

    private void handleOneToManyField(Field field, Object entity) throws IllegalAccessException {
        if (field.isAnnotationPresent(OneToMany.class)) {
            Collection<?> relatedEntities = (Collection<?>) field.get(entity);

            if (relatedEntities != null && !relatedEntities.isEmpty()) {
                for (Object relatedEntity : relatedEntities) {
                    Field relatedIdField = getIdField(relatedEntity.getClass());
                    relatedIdField.setAccessible(true);
                    Object relatedIdValue = relatedIdField.get(relatedEntity);
                    // Handle related entities as needed (e.g., saving to another table)
                }
            }
        }
    }

    private void handleManyToManyField(Field field, Object entity) throws IllegalAccessException, InterruptedException {
        if (field.isAnnotationPresent(ManyToMany.class)) {
            ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
            Collection<?> relatedEntities = (Collection<?>) field.get(entity);

            if (relatedEntities != null && !relatedEntities.isEmpty()) {
                for (Object relatedEntity : relatedEntities) {

                    Field relatedIdField = getIdField(relatedEntity.getClass());
                    relatedIdField.setAccessible(true);

                    Object relatedIdValue = relatedIdField.get(relatedEntity);
                    // Insert into the join table as needed
                    String joinTable = manyToMany.joinTable();
                    String joinColumn = manyToMany.joinColumn();
                    String inverseJoinColumn = manyToMany.inverseJoinColumn();


                    System.out.println(joinTable);
                    System.out.println(joinColumn);
                    System.out.println(inverseJoinColumn);
                    System.out.println(relatedIdValue);
                    Field xd = getIdField(entity.getClass());
                    xd.setAccessible(true);
                    Object xdId = xd.get(entity);
                    System.out.println(xd);
                    String joinQuery = String.format(
                            "INSERT INTO %s (%s, %s) VALUES ('%s', '%s')",
                            joinTable,
                            joinColumn,
                            inverseJoinColumn,
                            xdId,
                            relatedIdValue
                    );

                    try (Connection connection = connectionPool.getConnection();
                         PreparedStatement statement = connection.prepareStatement(joinQuery)) {
                        statement.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private Field getIdField(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Entity " + clazz.getName() + " must have @Id"));
    }


    private <T> void executeInsertQuery(String query, Class<?> clazz, T entity, String tableName) {
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Field idField = getIdField(clazz);
                    idField.setAccessible(true);
                    idField.set(entity, generatedKeys.getObject(1));
                }
            }
            connectionPool.notifyObservers("Sukces: Encja zapisana w tabeli " + tableName);
        } catch (Exception e) {
            throw new RuntimeException("Insert Query Execution Error: " + e.getMessage());
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
            throw new RuntimeException("Class " + clazz.getName() + " is not mapped in DB");
        }

        String tableName = table.name();
        String idColumn = null;
        Object idValue = id;

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true); // Ustaw dostęp do pola
            if (field.isAnnotationPresent(Id.class)) {
                Column column = field.getAnnotation(Column.class);
                idColumn = (column != null) ? column.name() : field.getName();
                break;
            }
        }

        if (idColumn == null) {
            throw new RuntimeException("Class " + clazz.getName() + " has no @Id field");
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

                    if (field.isAnnotationPresent(ManyToMany.class)) {
                        handleManyToManyField(field, entity);
                    }
                }
                return entity;
            }

        } catch (Exception e) {
            throw new RuntimeException("Find Query Execution Error: " + e.getMessage());
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
            connectionPool.notifyObservers("Zaktualizowano encję w tabeli " + tableName);
        } catch (Exception e) {
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
            connectionPool.notifyObservers("Usunięto encję z tabeli " + tableName + " o id: " + idValue);
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas usuwania encji: " + e.getMessage());
        }
    }
}
