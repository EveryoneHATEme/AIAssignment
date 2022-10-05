import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws WrongInputException {
        Map map = new Map(true);
        map.print();

        Decider decider = new Decider(map, Scenarios.SPYGLASS);
        List<Vector<Integer>> path = decider.findPath();

        for (Vector<Integer> pos : path)
            System.out.println(pos);
    }
}


class Vector<T> {
    public T x;
    public T y;

    public Vector() {

    }

    public Vector(T x, T y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector)
            return ((Vector<?>) obj).x.equals(x) && ((Vector<?>) obj).y.equals(y);
        else
            return false;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}


enum Scenarios {
    SPYGLASS,
    SUPER_SPYGLASS
}


class Decider {
    private Vector<Integer> actorPosition;
    private Map map;
    private Scenarios scenario;

    public Decider(Map map, Scenarios scenario) {
        this.map = map;
        this.scenario = scenario;

        actorPosition = map.getJackPosition();
    }

    public List<Vector<Integer>> findPath() {
        switch (this.scenario) {
            case SPYGLASS:
                return findPathSpyglass();
            case SUPER_SPYGLASS:
                return findPathSuperSpyglass();
            default:
                return new ArrayList<>();
        }
    }

    private List<Vector<Integer>> findPathSpyglass() {
        return findPathSpyglassBacktrack(actorPosition, new ArrayList<>());
    }

    private List<Vector<Integer>> findPathSpyglassBacktrack(Vector<Integer> currentPosition, List<Vector<Integer>> path) {
        List<Vector<Integer>> newPath = new ArrayList<>(path);              // copy path to a new list
        newPath.add(currentPosition);                                       // add current position

        if (map.isChestPosition(currentPosition))                           // path found
            return newPath;

        // list of all neighbor cells
        List<Vector<Integer>> neighborPositions = new ArrayList<>(
                List.of(new Vector<>(currentPosition.x - 1, currentPosition.y - 1),
                        new Vector<>(currentPosition.x, currentPosition.y - 1),
                        new Vector<>(currentPosition.x + 1, currentPosition.y - 1),
                        new Vector<>(currentPosition.x - 1, currentPosition.y),
                        new Vector<>(currentPosition.x + 1, currentPosition.y),
                        new Vector<>(currentPosition.x - 1, currentPosition.y + 1),
                        new Vector<>(currentPosition.x, currentPosition.y + 1),
                        new Vector<>(currentPosition.x + 1, currentPosition.y + 1))
        );

//        neighborPositions = neighborPositions.stream().filter(
//                position -> !currentPosition.equals(position)   // delete positions that are already presented in the path
//        ).filter(
//                Map::isOnMap                                    // delete positions that are out of bound
//        ).filter(
//                position -> !map.isDanger(position)             // delete dangerous positions
//        ).collect(Collectors.toList());
        // TODO: fix bug
        neighborPositions = neighborPositions.stream().filter(position -> !currentPosition.equals(position)).collect(Collectors.toList());
        neighborPositions = neighborPositions.stream().filter(Map::isOnMap).collect(Collectors.toList());
        neighborPositions = neighborPositions.stream().filter(position -> !map.isDanger(position)).collect(Collectors.toList());

        // for each neighbor position find path
        List<List<Vector<Integer>>> pathsObtained = neighborPositions.stream().map(
                position -> findPathSpyglassBacktrack(position, newPath)
        ).filter(
                Objects::nonNull
        ).collect(Collectors.toList());

        return pathsObtained.stream().min(Comparator.comparingInt(List::size)).orElse(null);
    }

    private List<Vector<Integer>> findPathSpyglassAStar() {
        return null;
    }

    private List<Vector<Integer>> findPathSuperSpyglass() {
        List<Vector<Integer>> result = new ArrayList<>();

        return result;
    }

    private List<Vector<Integer>> findPathSuperSpyglassBacktrack() {
        return null;
    }

    private List<Vector<Integer>> findPathSuperSpyglassAStar() {
        return null;
    }
}


class Map {
    private final List<List<Entity>> map;
    private static final Vector<Integer> size = new Vector<>(9, 9);
    private List<Entity> entities;
    private List<Attacker> attackers;
    private JackSparrow jack;
    private DeadMansChest chest;
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
        entities = new ArrayList<>();
        attackers = new ArrayList<>();
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

        jack = new JackSparrow();
        DavyJones davy = new DavyJones();
        Kraken kraken = new Kraken();
        Rock rock = new Rock();
        chest = new DeadMansChest();
        Tortuga tortuga = new Tortuga();

        entities = new ArrayList<>(List.of(
                jack,
                davy,
                kraken,
                rock,
                chest,
                tortuga
        ));

        attackers.addAll(List.of(davy, kraken, rock));

        String[] splitted;
        int x, y;
        for (int i = 0; i < splittedRow.length; ++i) {
            System.out.println(splittedRow[i]);
            splitted = splittedRow[i].split(",");

            x = Integer.parseInt(splitted[0]);
            y = Integer.parseInt(splitted[1]);

            if (!isOnMap(x, y))
                throw new WrongInputException();

            entities.get(i).setPosition(new Vector<>(x, y));

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

    public boolean isDanger(int x, int y) {
        for (Attacker attacker : attackers)
            if (attacker.isAttacking(x, y))
                return true;
        return false;
    }

    public boolean isDanger(Vector<Integer> position) {
        return isDanger(position.x, position.y);
    }

    public boolean isChestPosition(int x, int y) {
        return chest.getX() == x && chest.getY() == y;
    }

    public boolean isChestPosition(Vector<Integer> position) {
        return chest.getPosition().equals(position);
    }

    public Vector<Integer> getJackPosition() {
        return jack.getPosition();
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
    public List<Vector<Integer>> getAttackRange();
    public boolean isAttacking(int x, int y);
    public boolean isNeutralized();
    public void neutralize();
}


class JackSparrow implements Entity {
    private Vector<Integer> position;

    public JackSparrow() {
        position = new Vector<>();
    }

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


class DavyJones implements Entity, Attacker {
    private Vector<Integer> position;

    public DavyJones() {
        position = new Vector<>();
    }

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

    @Override
    public boolean isAttacking(int x, int y) {
        return Math.abs(x - this.position.x) == 1 && Math.abs(y - this.position.y) == 1;
    }

    @Override
    public boolean isNeutralized() {
        return false;
    }

    @Override
    public void neutralize() {

    }
}


class Kraken implements Entity, Attacker {
    private Vector<Integer> position;
    private boolean isNeutralized = false;

    public Kraken() {
        position = new Vector<>();
    }

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
        if (isNeutralized)
            return new ArrayList<>();
        else
            return new ArrayList<>(List.of(
                    new Vector<>(position.x - 1, position.y),
                    new Vector<>(position.x + 1, position.y),
                    new Vector<>(position.x, position.y - 1),
                    new Vector<>(position.x, position.y + 1)
            ));
    }

    @Override
    public boolean isAttacking(int x, int y) {
        System.out.println("my pos: " + position);
        boolean res = Math.abs(x - position.x) == 1 && y == position.y
                || Math.abs(y - position.y) == 1 && x == position.x;
        System.out.println("position " + x + " " + y + " is attacked: " + res);
        return res;
    }

    @Override
    public boolean isNeutralized() {
        return isNeutralized;
    }

    @Override
    public void neutralize() {
        isNeutralized = true;
    }
}


class Rock implements Entity, Attacker {
    private Vector<Integer> position;

    public Rock() {
        position = new Vector<>();
    }

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
        return new ArrayList<>(List.of(position));
    }

    @Override
    public boolean isAttacking(int x, int y) {
        return position.x == x && position.y == y;
    }

    @Override
    public boolean isNeutralized() {
        return false;
    }

    @Override
    public void neutralize() {

    }
}


class DeadMansChest implements Entity {
    private Vector<Integer> position;

    public DeadMansChest() {
        position = new Vector<>();
    }

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

    public Tortuga() {
        position = new Vector<>();
    }

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