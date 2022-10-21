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
        Path path = decider.findPath();

        for (Vector<Integer> pos : path.getPositions())
            System.out.println(pos);
    }
}


class Vector<T> {
    /*
    * Defines a container for two values
    * Used in code to store positions of entities on a map
    * The fields x and y are public and have no getters and setters, because it is sufficient for just storing some values
    * */
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


class Path {
    private final List<Vector<Integer>> positions;
    private boolean rumPicked;

    public Path() {
        positions = new ArrayList<>();
        rumPicked = false;
    }

    public Path(Path oldPath) {
        positions = new ArrayList<>(oldPath.getPositions());
        rumPicked = oldPath.isRumPicked();
    }

    public List<Vector<Integer>> getPositions() {
        return positions;
    }

    public void addPosition(Vector<Integer> position) {
        positions.add(position);
    }

    public void addPositionToBeginning(Vector<Integer> position) {
        positions.add(0, position);
    }

    public void extend(Path otherPath) {
        for (Vector<Integer> position : otherPath.positions)
            addPosition(position);
    }

    public boolean isRumPicked() {
        return rumPicked;
    }

    public void setRumPicked(boolean picked) {
        rumPicked = picked;
    }

    public int size() {
        return positions.size();
    }

    public boolean contains(Vector<Integer> position) {
        for (Vector<Integer> pathPosition : positions)
            if (position.equals(pathPosition))
                return true;
        return false;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for (Vector<Integer> pos : positions)
            res.append(pos.toString());
        return res.toString();
    }
}


enum Scenarios {
    SPYGLASS,
    SUPER_SPYGLASS
}


class Decider {
    private final Vector<Integer> actorPosition;
    private final Map map;
    private final Scenarios scenario;

    public Decider(Map map, Scenarios scenario) {
        this.map = map;
        this.scenario = scenario;

        actorPosition = map.getJackPosition();
    }

    public Path findPath() {
        switch (this.scenario) {
            case SPYGLASS:
                return findPathSpyglass();
            case SUPER_SPYGLASS:
                return findPathSuperSpyglass();
            default:
                return new Path();
        }
    }

    private Path findPathSpyglass() {
        int[][] pathLengthMatrix = Utils.createMatrix(Map.getSize().x, Map.getSize().y, -1);

        findPathSpyglassBacktrack(actorPosition, 0, pathLengthMatrix, false, false);

        Vector<Integer> chestPosition = map.getChestPosition();
        Vector<Integer> tortugaPosition = map.getTortugaPosition();
        Path pathToChest = new Path();

        if (pathLengthMatrix[chestPosition.x][chestPosition.y] != -1) {
            pathToChest = getPathAfterBacktracking(map.getJackPosition(), chestPosition, pathLengthMatrix);
        } else if (pathLengthMatrix[tortugaPosition.x][tortugaPosition.y] != -1) {
            pathToChest = getPathAfterBacktracking(map.getJackPosition(), tortugaPosition, pathLengthMatrix);

            pathLengthMatrix = Utils.createMatrix(Map.getSize().x, Map.getSize().y, -1);

            findPathSpyglassBacktrack(tortugaPosition, 0, pathLengthMatrix, true, false);
            if (pathLengthMatrix[chestPosition.x][chestPosition.y] == -1)
                return new Path();
            Path pathFromTortugaToChest = getPathAfterBacktracking(tortugaPosition, chestPosition, pathLengthMatrix);
            pathToChest.extend(pathFromTortugaToChest);
        }

        return pathToChest;
    }

    private void findPathSpyglassBacktrack(Vector<Integer> currentPosition, int currentPathLength, int[][] pathLengthMatrix, boolean rumPicked, boolean krakenKilled) {
        if (pathLengthMatrix[currentPosition.x][currentPosition.y] > currentPathLength || pathLengthMatrix[currentPosition.x][currentPosition.y] == -1)
            pathLengthMatrix[currentPosition.x][currentPosition.y] = currentPathLength;
        else
            return;

        List<Vector<Integer>> neighborCells = List.of(
                new Vector<>(currentPosition.x - 1, currentPosition.y - 1),
                new Vector<>(currentPosition.x, currentPosition.y - 1),
                new Vector<>(currentPosition.x + 1, currentPosition.y - 1),
                new Vector<>(currentPosition.x - 1, currentPosition.y),
                new Vector<>(currentPosition.x + 1, currentPosition.y),
                new Vector<>(currentPosition.x - 1, currentPosition.y + 1),
                new Vector<>(currentPosition.x, currentPosition.y + 1),
                new Vector<>(currentPosition.x + 1, currentPosition.y + 1)
        );

        List<Vector<Integer>> filteredNeighborCells = new ArrayList<>();
        for (Vector<Integer> cell : neighborCells) {
            if (!Map.isOnMap(cell))
                continue;

            if (map.isDanger(currentPosition, krakenKilled))
                continue;

            if (pathLengthMatrix[cell.x][cell.y] <= currentPathLength && pathLengthMatrix[cell.x][cell.y] != -1)
                continue;

            filteredNeighborCells.add(cell);
        }

        for (Vector<Integer> cell : filteredNeighborCells) {
            if (rumPicked && map.isKrakenPosition(cell))
                findPathSpyglassBacktrack(cell, currentPathLength + 1, pathLengthMatrix, true, true);
            else
                findPathSpyglassBacktrack(cell, currentPathLength + 1, pathLengthMatrix, rumPicked, krakenKilled);
        }
    }

    private Path getPathAfterBacktracking(Vector<Integer> from, Vector<Integer> target, int[][] minLengthMatrix) {
        Vector<Integer> currentPosition = target;
        Path pathToTarget = new Path();
        boolean nextStep = false;
        while (!currentPosition.equals(from)) {
            for (int i = currentPosition.x - 1; i <= currentPosition.x + 1; ++i) {
                for (int j = currentPosition.y - 1; j <= currentPosition.y + 1; ++j) {
                    if (i == currentPosition.x && j == currentPosition.y)
                        continue;

                    if (!Map.isOnMap(i, j))
                        continue;

                    if (minLengthMatrix[i][j] == minLengthMatrix[currentPosition.x][currentPosition.y] - 1) {
                        pathToTarget.addPositionToBeginning(currentPosition);
                        currentPosition = new Vector<>(i, j);
                        nextStep = true;
                    }

                    if (nextStep)
                        break;
                }
                if (nextStep)
                    break;
            }
            nextStep = false;
        }

        return pathToTarget;
    }

    private Path findPathSpyglassAStar() {
        return null;
    }

    private Path findPathSuperSpyglass() {
        return findPathSpyglass();
    }

    private Path findPathSuperSpyglassBacktrack() {
        return null;
    }

    private Path findPathSuperSpyglassAStar() {
        return null;
    }
}


class Map {
    private final List<List<Entity>> map;
    private static final Vector<Integer> size = new Vector<>(9, 9);
    private List<Entity> entities;
    private final List<Attacker> attackers;
    private JackSparrow jack;
    private DeadMansChest chest;
    private Tortuga tortuga;
    private Kraken kraken;
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
        kraken = new Kraken();
        Rock rock = new Rock();
        chest = new DeadMansChest();
        tortuga = new Tortuga();

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

    public static Vector<Integer> getSize() {
        return size;
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

    public boolean isDanger(int x, int y, boolean krakenKilled) {
        for (Attacker attacker : attackers)
            if (attacker.isAttacking(x, y) && !(attacker instanceof Kraken && krakenKilled))
                return true;
        return false;
    }

    public boolean isDanger(Vector<Integer> position, boolean rumPicked) {
        return isDanger(position.x, position.y, rumPicked);
    }

    public Vector<Integer> getChestPosition() {
        return chest.getPosition();
    }

    public boolean isKrakenPosition(Vector<Integer> position) {
        return position.equals(kraken.getPosition());
    }

    public boolean isChestPosition(int x, int y) {
        return chest.getX() == x && chest.getY() == y;
    }

    public boolean isChestPosition(Vector<Integer> position) {
        return chest.getPosition().equals(position);
    }

    public Vector<Integer> getTortugaPosition() {
        return tortuga.getPosition();
    }

    public boolean isTortugaPosition(int x, int y) {
        return tortuga.getX() == x && tortuga.getY() == y;
    }

    public boolean isTortugaPosition(Vector<Integer> position) {
        return tortuga.getPosition().equals(position);
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
                else if (isDanger(j, i, false))
                    System.out.print('#');
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
    public boolean canBeNeutralized(int x, int y);
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
        for (Vector<Integer> position : getAttackRange())
            if (position.x == x && position.y == y)
                return true;
        return false;
    }

    public boolean canBeNeutralized(int x, int y) {
        return false;
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
                    new Vector<>(position.x, position.y),
                    new Vector<>(position.x - 1, position.y),
                    new Vector<>(position.x + 1, position.y),
                    new Vector<>(position.x, position.y - 1),
                    new Vector<>(position.x, position.y + 1)
            ));
    }

    @Override
    public boolean isAttacking(int x, int y) {
        return Math.abs(x - position.x) <= 1 && y == position.y
                || Math.abs(y - position.y) <= 1 && x == position.x;
    }

    public boolean canBeNeutralized(int x, int y) {
        return (position.y - 1 == y || position.y + 1 == y) && (position.x - 1 == x || position.x + 1 == x);
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


    public boolean canBeNeutralized(int x, int y) {
        return false;
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


class Utils {
    public static int[][] createMatrix(int sizeX, int sizeY, int defaultValue) {
        int[][] matrix = new int[sizeX][sizeY];

        for (int i = 0; i < sizeX; ++i)
            for (int j = 0; j < sizeY; ++j)
                matrix[i][j] = defaultValue;

        return matrix;
    }

    public static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix[0].length; ++i) {
            for (int[] line : matrix)
                System.out.print(line[i] + "\t");
            System.out.println();
        }
        System.out.println();
    }
}


class WrongInputException extends Exception {

}