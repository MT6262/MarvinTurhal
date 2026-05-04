package org.example;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.SQLException;
import java.util.List;

@DatabaseTable(tableName = "letters")
class Letter {
    @DatabaseField(id = true)
    private int id;

    @DatabaseField
    private String letter;

    public Letter() {

    }

    public int getId() {
        return id;
    }

    public String getLetter() {
        return letter;
    }
}

public class Main {
    private static final String DATABASE_URL = "jdbc:mariadb://bilbao.informatik.uni-stuttgart.de/pe2-db-a1";
    private static final String USERNAME = "pe2-nutzer";
    private static final String PASSWORD = "esJLtFm6ksCT4mCyOS";

    public static void main(String[] args) {
        int[] arrayIndexes = {
            20, 44, 50, 13, 17, 33, 41,
            68, 77, 44, 29, 72, 48, 71,
            37, 48, 11, 69, 5, 65, 65
        };

        try {
            JdbcConnectionSource connectionSource = new JdbcConnectionSource(DATABASE_URL, USERNAME, PASSWORD);
            Dao<Letter, Integer> letterDao = DaoManager.createDao(connectionSource, Letter.class);

            // Find and print the word
            StringBuilder word = new StringBuilder();
            for (int id : arrayIndexes) {
                Letter letter = letterDao.queryForId(id);
                if (letter != null) {
                    word.append(letter.getLetter());
                } else {
                    System.out.println("Kein Buchstabe für ID: " + id);
                }
            }
            System.out.println("Das Lösungswort ist: " + word.toString());

            // Find IDs for specific letters
            findIdsForLetter(letterDao, "V");
            findIdsForLetter(letterDao, "b");
            findIdsForLetter(letterDao, "t");

            // Sum and average of all IDs
            List<Letter> letters = letterDao.queryForAll();
            int sum = 0;
            for (Letter letter : letters) {
                sum += letter.getId();
            }
            double average = letters.isEmpty() ? 0 : (double) sum / letters.size();
            System.out.println("Summe der IDs: " + sum);
            System.out.println("Durchschnittswert der IDs: " + average);

            connectionSource.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void findIdsForLetter(Dao<Letter, Integer> letterDao, String letter) throws SQLException {
        List<Letter> letters = letterDao.queryForEq("letter", letter);
        System.out.println("IDs for letter '" + letter + "':");
        for (Letter l : letters) {
            System.out.println(l.getId());
        }
    }
}