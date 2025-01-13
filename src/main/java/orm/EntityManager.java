package orm;

import orm.annotations.*;
import orm.logging.LoggerObserver;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Klasa EntityManager zarządzająca operacjami CRUD na encjach.
 */
public class EntityManager {

    private final ConnectionPool connectionPool;

    public EntityManager(LoggerObserver loggerObserver) throws SQLException {
        this.connectionPool = ConnectionPool.getInstance();
        connectionPool.addObserver(loggerObserver);
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
                }
            }
        }
    }

    private void handleManyToManyField(Field field, Object entity) throws IllegalAccessException {
        if (field.isAnnotationPresent(ManyToMany.class)) {
            ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
            Collection<?> relatedEntities = (Collection<?>) field.get(entity);

            if (relatedEntities != null && !relatedEntities.isEmpty()) {
                String joinTable = manyToMany.joinTable();
                String joinColumn = manyToMany.joinColumn();
                String inverseJoinColumn = manyToMany.inverseJoinColumn();

                Field idField = getIdField(entity.getClass());
                idField.setAccessible(true);
                Object entityId = idField.get(entity);

                for (Object relatedEntity : relatedEntities) {
                    Field relatedIdField = getIdField(relatedEntity.getClass());
                    relatedIdField.setAccessible(true);
                    Object relatedId = relatedIdField.get(relatedEntity);

                    String query = String.format(
                            "INSERT INTO %s (%s, %s) VALUES (?, ?) ON CONFLICT DO NOTHING",
                            joinTable, joinColumn, inverseJoinColumn
                    );

                    try (Connection connection = connectionPool.getConnection();
                         PreparedStatement statement = connection.prepareStatement(query)) {
                        statement.setObject(1, entityId);
                        statement.setObject(2, relatedId);
                        statement.executeUpdate();
                    } catch (SQLException | InterruptedException e) {
                        throw new RuntimeException("Failed to save ManyToMany relationship: " + e.getMessage(), e);
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
        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            PreparedStatement statement = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Field idField = getIdField(clazz);
                    idField.setAccessible(true);
                    idField.set(entity, generatedKeys.getObject(1));
                }
            }
            connectionPool.notifyObservers("Encja zapisana w tabeli " + tableName + ": " + entity.toString());
        } catch (Exception e) {
            throw new RuntimeException("Insert Query Execution Error: " + e.getMessage());
        }  finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
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
            field.setAccessible(true);
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
        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
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

                    if(field.isAnnotationPresent(OneToOne.class)) {
                        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                        if(!oneToOne.foreignKeyInThisTable()) {
                            var found = findOneToOne(field.getType(), id, oneToOne.column());
                            field.set(entity, found);
                        }
                    }

                    if(field.isAnnotationPresent(OneToMany.class)) {
                        OneToMany oneToMany = field.getAnnotation(OneToMany.class);

                        ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
                        Class<?> listClass = (Class<?>) stringListType.getActualTypeArguments()[0];

                        var found = findOneToMany(listClass, id, oneToMany.mappedBy());
                        field.set(entity, found);
                    }

                    if(field.isAnnotationPresent(ManyToOne.class)) {
                        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                        Object relatedIdValue = resultSet.getObject(manyToOne.column());
                        if (relatedIdValue != null) {
                            var found = findManyToOne(field.getType(), relatedIdValue);
                            field.set(entity, found);
                        }
                    }
                }
                return entity;
            }
        } catch (Exception e) {
            throw new RuntimeException("Find Query Execution Error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }

        return null;
    }

    private  <T> T findOneToOne(Class<T> clazz, Object id, String columnName) {
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("Class " + clazz.getName() + " is not mapped in DB");
        }

        String tableName = table.name();
        String idColumn = null;
        Object idValue = id;

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(OneToOne.class)) {
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                var idColumnTemp = oneToOne.column();
                if(Objects.equals(columnName, idColumnTemp)){
                    idColumn = idColumnTemp;
                    break;
                }
            }
        }

        if (idColumn == null) {
            throw new RuntimeException("Class " + clazz.getName() + " has no @Id field");
        }

        String query = String.format("SELECT * FROM %s WHERE %s = ?", tableName, idColumn);

        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);

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
                }
                return entity;
            }
        } catch (Exception e) {
            throw new RuntimeException("Find Query Execution Error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }

        return null;
    }

    private  <T> List<T> findOneToMany(Class<T> clazz, Object id, String columnName) {
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("Class " + clazz.getName() + " is not mapped in DB");
        }

        String tableName = table.name();
        String idColumn = null;

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ManyToOne.class)) {
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                var idColumnTemp = manyToOne.column();
                if(Objects.equals(columnName, idColumnTemp)){
                    idColumn = idColumnTemp;
                    break;
                }
            }
        }

        if (idColumn == null) {
            throw new RuntimeException("Class " + clazz.getName() + " has no @Id field");
        }

        String query = String.format("SELECT * FROM %s WHERE %s = ?", tableName, idColumn);

        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setObject(1, id);
            ResultSet resultSet = statement.executeQuery();

            List<T> entities = new ArrayList<>();
            if (resultSet.next()) {
                T entity = clazz.getDeclaredConstructor().newInstance();
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);

                    if (field.isAnnotationPresent(Column.class)) {
                        Column column = field.getAnnotation(Column.class);
                        field.set(entity, resultSet.getObject(column.name()));
                    }
                }
                entities.add(entity);
            }
            return entities;
        } catch (Exception e) {
            throw new RuntimeException("Find Query Execution Error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
    }


    private  <T> T findManyToOne(Class<T> clazz, Object id) {
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("Class " + clazz.getName() + " is not mapped in DB");
        }

        String tableName = table.name();
        String idColumn = null;

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
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

        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setObject(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                T entity = clazz.getDeclaredConstructor().newInstance();

                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);

                    if (field.isAnnotationPresent(Column.class)) {
                        Column column = field.getAnnotation(Column.class);
                        field.set(entity, resultSet.getObject(column.name()));
                    }
                }
                return entity;
            }
        } catch (Exception e) {
            throw new RuntimeException("Find Query Execution Error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
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

                if (field.isAnnotationPresent(ManyToOne.class)) {
                    ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                    Object relatedEntity = field.get(entity);

                    if (relatedEntity != null) {
                        Field relatedIdField = getIdField(relatedEntity.getClass());
                        relatedIdField.setAccessible(true);
                        Object relatedIdValue = relatedIdField.get(relatedEntity);

                        setClause.append(manyToOne.column()).append(" = '").append(relatedIdValue).append("',");
                    }
                }

                if (field.isAnnotationPresent(OneToOne.class)) {
                    OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                    Object relatedEntity = field.get(entity);

                    if (relatedEntity != null) {
                        Field relatedIdField = getIdField(relatedEntity.getClass());
                        relatedIdField.setAccessible(true);
                        Object relatedIdValue = relatedIdField.get(relatedEntity);

                        if(oneToOne.foreignKeyInThisTable()) {
                            setClause.append(oneToOne.column()).append(" = '").append(relatedIdValue).append("',");
                        }
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
            Connection connection = null;
            try {
                connection = connectionPool.getConnection();
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setObject(1, idValue);
                statement.executeUpdate();

                connectionPool.notifyObservers("Zaktualizowano encję w tabeli " + tableName);
            } finally {
                if (connection != null) {
                    connectionPool.releaseConnection(connection);
                }
            }
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

            Connection connection = null;
            try {
                connection = connectionPool.getConnection();
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setObject(1, idValue);
                statement.executeUpdate();

                connectionPool.notifyObservers("Usunięto encję z tabeli " + tableName + " o id: " + idValue);
            } finally {
                if (connection != null) {
                    connectionPool.releaseConnection(connection);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas usuwania encji: " + e.getMessage());
        }
    }


    /**
     * Wykonuje customowe zapytanie SELECT i mapuje wyniki na encje.
     *
     * @param query zapytanie SQL do wykonania
     * @param clazz klasa encji, na którą ma być mapowany wynik
     * @param params opcjonalne parametry zapytania
     * @param <T> typ encji
     * @return lista obiektów encji lub pusta lista, jeśli brak wyników
     */
    public <T> List<T> executeQuery(String query, Class<T> clazz, Object... params) {
        List<T> results = new ArrayList<>();

        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            System.out.println(statement);
            ResultSet resultSet = statement.executeQuery();


            while (resultSet.next()) {
                T entity = clazz.getDeclaredConstructor().newInstance();
                Object idValue = null;
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);

                    if (field.isAnnotationPresent(Id.class)) {
                        Column column = field.getAnnotation(Column.class);
                        idValue = resultSet.getObject(column.name());
                    }
                    if (idValue == null) {
                        throw new RuntimeException("Class " + clazz.getName() + " has no @Id field");
                    }

                    if (field.isAnnotationPresent(Column.class)) {
                        Column column = field.getAnnotation(Column.class);
                        field.set(entity, resultSet.getObject(column.name()));
                        System.out.println(resultSet.getObject(column.name()));
                    }

                    if (field.isAnnotationPresent(OneToOne.class)) {
                        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                        var found = findOneToOne(field.getType(), idValue, oneToOne.column());
                        System.out.println(idValue);
                        field.set(entity, found);
                    }
                    if (field.isAnnotationPresent(ManyToOne.class)) {
                        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                        Object relatedEntity = findManyToOne(field.getType(), resultSet.getObject(manyToOne.column()));
                        field.set(entity, relatedEntity);
                    }
                }

                results.add(entity);
            }
        } catch (Exception e) {
            throw new RuntimeException("Custom Query Execution Error: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }

        return results;
    }


    /**
     * Wykonuje customowe zapytanie modyfikujące dane (INSERT, UPDATE, DELETE).
     *
     * @param query zapytanie SQL do wykonania
     * @param params opcjonalne parametry zapytania
     * @return liczba zmodyfikowanych wierszy
     */
    public int executeUpdate(String query, Object... params) {
        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);

            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement.executeUpdate();
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException("Custom Update Query Execution Error: " + e.getMessage(), e);
        }finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
    }


}
