import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws WrongInputException {
        Map map = new Map(true);
        map.print();
    }
}


class Vector<T> {
    public T x;
    public T y;

    Vector(T x, T y) {
        this.x = x;
        this.y = y;
    }
}


class Map {
    private final List<List<Entity>> map;
    private static final Vector<Integer> size = new Vector<>(9, 9);
    private List<Entity> entities;


    enum Scenarios {
        SPYGLASS,
        SUPER_SPYGLASS
    }

    private Scenarios scenario;

    private Map() {
        map = new ArrayList<>(size.x);
        List<Entity> line;
        for (int i = 0; i < size.y; ++i) {
            line = new ArrayList<>(size.y);
            for (int j = 0; j < size.x; ++j)
                line.add(null);

            map.add(line);
        }
    }

    public Map(boolean readFromFile) throws WrongInputException {
        this();
        if (readFromFile)
            readDataFromFile("input.txt");
        else
            readDataFromConsole();
    }

    private void readDataFromFile(String filename) throws WrongInputException {
        String firstLine;
        String secondLine;

        try {
            File file = new File(filename);
            Scanner scanner = new Scanner(file);

            firstLine = scanner.nextLine();
            secondLine = scanner.nextLine();

            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            e.printStackTrace();
            return;
        }

        handleInputData(firstLine, secondLine);
    }

    private void readDataFromConsole() throws WrongInputException {
        String firstLine;
        String secondLine;

        Scanner scanner = new Scanner(System.in);

        firstLine = scanner.nextLine();
        secondLine = scanner.nextLine();

        scanner.close();

        handleInputData(firstLine, secondLine);
    }

    private void handleInputData(String firstLine, String secondLine) throws WrongInputException {
        firstLine = firstLine.substring(1, firstLine.length() - 1);
        String[] splittedRow = firstLine.split(Pattern.quote("] ["));

        if (splittedRow.length != 6)
            throw new WrongInputException();

        entities = new ArrayList<>(List.of(
                new Entity[]{
                        new JackSparrow(),
                        new DavyJones(),
                        new Kraken(),
                        new Rock(),
                        new DeadMansChest(),
                        new Tortuga()
                }
        ));

        String[] splitted;
        int x, y;
        for (int i = 0; i < splittedRow.length; ++i) {
            System.out.println(splittedRow[i]);
            splitted = splittedRow[i].split(",");

            x = Integer.parseInt(splitted[0]);
            y = Integer.parseInt(splitted[1]);

            if (!isOnMap(x, y))
                throw new WrongInputException();

            entities.get(i).setX(x);
            entities.get(i).setY(y);

            setCell(x, y, entities.get(i));
        }

        int scenarioNumber = Integer.parseInt(secondLine);
        switch (scenarioNumber) {
            case 1:
                scenario = Scenarios.SPYGLASS;
                break;
            case 2:
                scenario = Scenarios.SUPER_SPYGLASS;
                break;
        }
    }

    public Entity getCell(int x, int y) {
        if (!isOnMap(x, y))
            return null;

        return map.get(x).get(y);
    }

    public Entity getCell(Vector<Integer> vector) {
        return getCell(vector.x, vector.y);
    }

    public void setCell(int x, int y, Entity entity) {
        if (!isOnMap(x, y))
            return;

        map.get(x).set(y, entity);
        entity.setX(x);
        entity.setY(y);
    }

    public void setCell(Vector<Integer> vector, Entity entity) {
        setCell(vector.x, vector.y, entity);
    }

    public void print() {
        Entity currentCell;

        for (int i = 0; i < size.y; ++i) {
            for (int j = 0; j < size.x; ++j) {
                currentCell = getCell(j, i);

                if (currentCell instanceof JackSparrow)
                    System.out.print('J');
                else if (currentCell instanceof DavyJones)
                    System.out.print('D');
                else if (currentCell instanceof Kraken)
                    System.out.print('K');
                else if (currentCell instanceof Rock)
                    System.out.print('R');
                else if (currentCell instanceof DeadMansChest)
                    System.out.print('X');
                else if (currentCell instanceof Tortuga)
                    System.out.print('T');
                else
                    System.out.print('_');
            }

            System.out.print('\n');
        }
    }

    public static boolean isOnMap(int x, int y) {
        return x >= 0 && x < size.x && y >= 0 && y < size.y;
    }

    public static boolean isOnMap(Vector<Integer> vector) {
        return isOnMap(vector.x, vector.y);
    }
}


interface Entity {
    public int getX();

    public void setX(int x);

    public int getY();

    public void setY(int y);

    public Vector<Integer> getPosition();

    public void setPosition(Vector<Integer> position);
}

interface Attacker {
    public abstract List<Vector<Integer>> getAttackRange();
}


class JackSparrow implements Entity {
    private Vector<Integer> position;

    public int getX() {
        return position.x;
    }

    public void setX(int x) {
        this.position.x = x;
    }

    public int getY() {
        return this.position.y;
    }

    public void setY(int y) {
        this.position.y = y;
    }

    public Vector<Integer> getPosition() {
        return position;
    }

    public void setPosition(Vector<Integer> position) {
        this.position = position;
    }

    public List<Vector<Integer>> getAttackRange() {
        List<Vector<Integer>> attackRange = new ArrayList<>(8);
        for (int i = position.x - 1; i <= position.x + 1; ++i) {
            for (int j = position.y - 1; j <= position.y + 1; ++j)
                attackRange.add(new Vector<>(i, j));
        }
        return attackRange;
    }
}


class DavyJones implements Entity, Attacker {
    private Vector<Integer> position;

    public int getX() {
        return position.x;
    }

    public void setX(int x) {
        this.position.x = x;
    }

    public int getY() {
        return this.position.y;
    }

    public void setY(int y) {
        this.position.y = y;
    }

    public Vector<Integer> getPosition() {
        return position;
    }

    public void setPosition(Vector<Integer> position) {
        this.position = position;
    }

    public List<Vector<Integer>> getAttackRange() {
        List<Vector<Integer>> attackRange = new ArrayList<>(8);
        for (int i = position.x - 1; i <= position.x + 1; ++i) {
            for (int j = position.y - 1; j <= position.y + 1; ++j)
                attackRange.add(new Vector<>(i, j));
        }
        return attackRange;
    }
}


class Kraken implements Entity, Attacker {
    private Vector<Integer> position;

    public int getX() {
        return position.x;
    }

    public void setX(int x) {
        this.position.x = x;
    }

    public int getY() {
        return this.position.y;
    }

    public void setY(int y) {
        this.position.y = y;
    }

    public Vector<Integer> getPosition() {
        return position;
    }

    public void setPosition(Vector<Integer> position) {
        this.position = position;
    }

    @Override
    public List<Vector<Integer>> getAttackRange() {
        return new ArrayList<>(List.of(
                new Vector<>(position.x - 1, position.y),
                new Vector<>(position.x + 1, position.y),
                new Vector<>(position.x, position.y - 1),
                new Vector<>(position.x, position.y + 1)
        ));
    }
}


class Rock implements Entity, Attacker {
    private Vector<Integer> position;

    public int getX() {
        return position.x;
    }

    public void setX(int x) {
        this.position.x = x;
    }

    public int getY() {
        return this.position.y;
    }

    public void setY(int y) {
        this.position.y = y;
    }

    public Vector<Integer> getPosition() {
        return position;
    }

    public void setPosition(Vector<Integer> position) {
        this.position = position;
    }

    @Override
    public List<Vector<Integer>> getAttackRange() {
        return null;
    }
}


class DeadMansChest implements Entity {
    private Vector<Integer> position;

    public int getX() {
        return position.x;
    }

    public void setX(int x) {
        this.position.x = x;
    }

    public int getY() {
        return this.position.y;
    }

    public void setY(int y) {
        this.position.y = y;
    }

    public Vector<Integer> getPosition() {
        return position;
    }

    public void setPosition(Vector<Integer> position) {
        this.position = position;
    }
}


class Tortuga implements Entity {
    private Vector<Integer> position;

    public int getX() {
        return position.x;
    }

    public void setX(int x) {
        this.position.x = x;
    }

    public int getY() {
        return this.position.y;
    }

    public void setY(int y) {
        this.position.y = y;
    }

    public Vector<Integer> getPosition() {
        return position;
    }

    public void setPosition(Vector<Integer> position) {
        this.position = position;
    }
}


class WrongInputException extends Exception {

}