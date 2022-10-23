import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws WrongInputException {
        EnvironmentMap environmentMap = new EnvironmentMap(true);
        System.out.println(environmentMap.getString());

        Decider decider = new Decider(environmentMap);
        decider.findPath();
    }
}


enum Scenarios {
    SPYGLASS,
    SUPER_SPYGLASS
}


class Decider {
    private final EnvironmentMap environmentMap;

    public Decider(EnvironmentMap environmentMap) {
        this.environmentMap = environmentMap;
    }

    public void findPath(String outputFileNameBacktracking, String outputFileNameAStar) {
        Path backtrackingResult = new Path(), aStarResult = new Path();
        long backtrackingStartTime, backtrackingTimeTaken = 0, aStarStartTime, aStarTimeTaken = 0;
        switch (environmentMap.getScenario()) {
            case SUPER_SPYGLASS:
            case SPYGLASS:
                backtrackingStartTime = System.nanoTime();
                backtrackingResult = findPathSpyglassBacktrack();
                backtrackingTimeTaken = (System.nanoTime() - backtrackingStartTime) / 1000000;
                aStarStartTime = System.nanoTime();
                aStarResult = findPathSpyglassAStarHead();
                aStarTimeTaken = (System.nanoTime() - aStarStartTime) / 1000000;
        }

        boolean winBacktracking = backtrackingResult.size() != 0;
        printToFile(winBacktracking, backtrackingResult, backtrackingTimeTaken, outputFileNameBacktracking);

        boolean winAStar = aStarResult.size() != 0;
        printToFile(winAStar, aStarResult, aStarTimeTaken, outputFileNameAStar);
    }

    public void findPath() {
        findPath("outputBacktracking.txt", "outputAStar.txt");
    }

    public void printToFile(boolean win, Path path, long timeTaken, String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

            if (!win) {
                writer.write("Lose\n");
            } else {
                writer.write("Win\n");
                writer.append(String.valueOf(path.size())).append('\n');
                writer.append(path.toString()).append('\n');
                writer.append(environmentMap.getString(path));
                writer.append(String.valueOf(timeTaken)).append("ms\n");
            }
            writer.close();
        } catch (IOException exception) {
            System.out.println("Cannot open the file " + filename);
        }
    }

    private Path findPathSpyglassBacktrack() {
        int[][] pathLengthMatrix = Utils.createMatrix(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y, -1);

        findPathSpyglassBacktrackRecursion(environmentMap.getJackPosition(), 0, pathLengthMatrix, false, false);

        Position chestPosition = environmentMap.getChestPosition();
        Position tortugaPosition = environmentMap.getTortugaPosition();
        Path pathToChest = new Path();

        if (pathLengthMatrix[chestPosition.x][chestPosition.y] != -1) {
            pathToChest = getPathAfterSearching(environmentMap.getJackPosition(), chestPosition, pathLengthMatrix);
        } else if (pathLengthMatrix[tortugaPosition.x][tortugaPosition.y] != -1) {
            pathToChest = getPathAfterSearching(environmentMap.getJackPosition(), tortugaPosition, pathLengthMatrix);

            pathLengthMatrix = Utils.createMatrix(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y, -1);

            findPathSpyglassBacktrackRecursion(tortugaPosition, 0, pathLengthMatrix, true, false);
            if (pathLengthMatrix[chestPosition.x][chestPosition.y] == -1)
                return new Path();
            Path pathFromTortugaToChest = getPathAfterSearching(tortugaPosition, chestPosition, pathLengthMatrix);
            pathToChest.extend(pathFromTortugaToChest);
        }

        return pathToChest;
    }

    private void findPathSpyglassBacktrackRecursion(Position currentPosition, int currentPathLength, int[][] pathLengthMatrix, boolean rumPicked, boolean krakenKilled) {
        if (pathLengthMatrix[currentPosition.x][currentPosition.y] > currentPathLength || pathLengthMatrix[currentPosition.x][currentPosition.y] == -1)
            pathLengthMatrix[currentPosition.x][currentPosition.y] = currentPathLength;
        else
            return;

        List<Position> neighborCells = List.of(
                new Position(currentPosition.x - 1, currentPosition.y - 1),
                new Position(currentPosition.x, currentPosition.y - 1),
                new Position(currentPosition.x + 1, currentPosition.y - 1),
                new Position(currentPosition.x - 1, currentPosition.y),
                new Position(currentPosition.x + 1, currentPosition.y),
                new Position(currentPosition.x - 1, currentPosition.y + 1),
                new Position(currentPosition.x, currentPosition.y + 1),
                new Position(currentPosition.x + 1, currentPosition.y + 1)
        );

        neighborCells = neighborCells.stream().filter(EnvironmentMap::isOnMap).collect(Collectors.toList());

        if (rumPicked)
            for (Position cell : neighborCells)
                if (environmentMap.isKrakenPosition(cell)) {
                    krakenKilled = true;
                    break;
                }

        boolean finalKrakenKilled = krakenKilled;
        neighborCells = neighborCells.stream().filter(cell -> !environmentMap.isDanger(cell, finalKrakenKilled))
                .filter(cell -> !(pathLengthMatrix[cell.x][cell.y] <= currentPathLength && pathLengthMatrix[cell.x][cell.y] != -1))
                .collect(Collectors.toList());

        for (Position cell : neighborCells) {
            findPathSpyglassBacktrackRecursion(cell, currentPathLength + 1, pathLengthMatrix, rumPicked, krakenKilled);
        }
    }

    private Path getPathAfterSearching(Position from, Position target, int[][] minLengthMatrix) {
        Position currentPosition = target;
        Path pathToTarget = new Path();
        boolean nextStep = false;
        while (!currentPosition.equals(from)) {
            for (int i = currentPosition.x - 1; i <= currentPosition.x + 1; ++i) {
                for (int j = currentPosition.y - 1; j <= currentPosition.y + 1; ++j) {
                    if (i == currentPosition.x && j == currentPosition.y)
                        continue;

                    if (!EnvironmentMap.isOnMap(i, j))
                        continue;

                    if (minLengthMatrix[i][j] == minLengthMatrix[currentPosition.x][currentPosition.y] - 1) {
                        pathToTarget.addPositionToBeginning(currentPosition);
                        currentPosition = new Position(i, j);
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

    private Path findPathSpyglassAStarHead() {
        int[][] pathLengths = Utils.createMatrix(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y, -1);

        Position jackPosition = environmentMap.getJackPosition();
        Position chestPosition = environmentMap.getChestPosition();
        Position tortugaPosition = environmentMap.getTortugaPosition();

        findPathSpyglassAStarBody(jackPosition, chestPosition, pathLengths, false);

        Path pathObtained = new Path();
        if (pathLengths[chestPosition.x][chestPosition.y] != -1)
            pathObtained = getPathAfterSearching(jackPosition, chestPosition, pathLengths);
        else if (pathLengths[tortugaPosition.x][tortugaPosition.y] != -1) {
            pathObtained = getPathAfterSearching(jackPosition, tortugaPosition, pathLengths);
            pathLengths = Utils.createMatrix(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y, -1);
            findPathSpyglassAStarBody(tortugaPosition, chestPosition, pathLengths, true);
            if (pathLengths[chestPosition.x][chestPosition.y] == -1)
                return new Path();
            pathObtained.extend(getPathAfterSearching(tortugaPosition, chestPosition, pathLengths));
        }

        return pathObtained;
    }

    private void findPathSpyglassAStarBody(Position from, Position to, int[][] pathLengths, boolean rumPicked) {
        BinaryHeap heap = new BinaryHeap();
        pathLengths[from.x][from.y] = 0;

        Path currentPath = new Path();
        currentPath.addPosition(from);

        Path updatedPath;

        Position currentPosition;
        heap.insertElement(0, currentPath);

        int totalCost;

        while (!heap.isEmpty()) {
            currentPath = heap.extractMin();
            currentPosition = currentPath.getLastPosition();
            if (currentPosition.equals(to))
                break;

            if (!currentPath.isKrakenKilled()) {
                for (int i = currentPosition.x - 1; i <= currentPosition.x + 1; ++i) {
                    for (int j = currentPosition.y - 1; j <= currentPosition.y + 1; ++j) {
                        if (i == currentPosition.x && j == currentPosition.y)
                            continue;

                        if (!EnvironmentMap.isOnMap(i, j))
                            continue;

                        if (rumPicked && environmentMap.isKrakenPosition(new Position(i, j)))
                            currentPath.setKrakenKilled(true);
                    }
                }
            }

            for (int i = currentPosition.x - 1; i <= currentPosition.x + 1; ++i) {
                for (int j = currentPosition.y - 1; j <= currentPosition.y + 1; ++j) {
                    updatedPath = new Path(currentPath);
                    updatedPath.addPosition(new Position(i, j));

                    if (i == currentPosition.x && j == currentPosition.y)
                        continue;

                    if (!EnvironmentMap.isOnMap(i, j))
                        continue;

                    if (pathLengths[i][j] != -1)
                        continue;

                    if (environmentMap.isDanger(i, j, updatedPath.isKrakenKilled()))
                        continue;

                    pathLengths[i][j] = pathLengths[currentPosition.x][currentPosition.y] + 1;
                    totalCost = pathLengths[i][j] + Utils.getHeuristicDistance(updatedPath.getLastPosition(), to);

                    heap.insertElement(totalCost, updatedPath);
                }
            }
        }
    }

    private Path findPathSuperSpyglass() {
        return null;
    }

    private Path findPathSuperSpyglassBacktrack() {
        return null;
    }

    private Path findPathSuperSpyglassAStar() {
        return null;
    }
}


class EnvironmentMap {
    private static final Position size = new Position(9, 9);
    private List<Entity> entities;
    private final List<Attacker> attackers;
    private JackSparrow jack;
    private DeadMansChest chest;
    private Tortuga tortuga;
    private Kraken kraken;
    private Scenarios scenario;

    private EnvironmentMap() {
        entities = new ArrayList<>();
        attackers = new ArrayList<>();
    }

    public EnvironmentMap(Position jackPosition, Position davyPosition, Position krakenPosition, Position rockPosition, Position chestPosition, Position tortugaPosition, Scenarios scenario) {
        this();

        jack = new JackSparrow(jackPosition);
        DavyJones davy = new DavyJones(davyPosition);
        kraken = new Kraken(krakenPosition);
        Rock rock = new Rock(rockPosition);
        chest = new DeadMansChest(chestPosition);
        tortuga = new Tortuga(tortugaPosition);

        entities = new ArrayList<>(List.of(
                jack,
                davy,
                kraken,
                rock,
                chest,
                tortuga
        ));

        attackers.addAll(List.of(davy, kraken, rock));

        this.scenario = scenario;
    }

    public EnvironmentMap(boolean readFromFile) throws WrongInputException {
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
            splitted = splittedRow[i].split(",");

            x = Integer.parseInt(splitted[0]);
            y = Integer.parseInt(splitted[1]);

            if (!isOnMap(x, y))
                throw new WrongInputException();

            entities.get(i).setPosition(new Position(x, y));
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

    public static Position getSize() {
        return size;
    }

    public boolean isDanger(int x, int y, boolean krakenKilled) {
        for (Attacker attacker : attackers)
            if (attacker.isAttacking(x, y) && !(attacker instanceof Kraken && krakenKilled))
                return true;
        return false;
    }

    public boolean isDanger(Position position, boolean rumPicked) {
        return isDanger(position.x, position.y, rumPicked);
    }

    public Position getChestPosition() {
        return chest.getPosition();
    }

    public boolean isKrakenPosition(Position position) {
        return position.equals(kraken.getPosition());
    }

    public Position getTortugaPosition() {
        return tortuga.getPosition();
    }

    public Position getJackPosition() {
        return jack.getPosition();
    }

    public String getString() {
        return getString(new Path());
    }

    public String getString(Path path) {
        Entity entityToPrint;
        boolean positionToPrint;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < size.y; ++i) {
            for (int j = 0; j < size.x; ++j) {
                Position finalCurrentPosition = new Position(j, i);
                entityToPrint = entities.stream().filter(entity -> entity.isOnPosition(finalCurrentPosition)).findFirst().orElse(null);
                positionToPrint = path.getPositions().stream().anyMatch(position -> position.equals(finalCurrentPosition));
                if (entityToPrint != null)
                    result.append(entityToPrint);
                else if (positionToPrint)
                    result.append('*');
                else
                    result.append('_');
            }
            result.append('\n');
        }
        return result.toString();
    }

    public Scenarios getScenario() {
        return scenario;
    }

    public static boolean isOnMap(int x, int y) {
        return x >= 0 && x < size.x && y >= 0 && y < size.y;
    }

    public static boolean isOnMap(Position position) {
        return isOnMap(position.x, position.y);
    }
}


abstract class Entity {
    protected Position position;

    public Entity() {
        this.position = new Position();
    }

    public Entity(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public boolean isOnPosition(Position position) {
        return this.position.equals(position);
    }

    @Override
    public abstract String toString();
}

interface Attacker {
    boolean isAttacking(int x, int y);
}


class JackSparrow extends Entity {
    public JackSparrow() {
        super();
    }

    public JackSparrow(Position jackPosition) {
        super(jackPosition);
    }

    @Override
    public String toString() {
        return "J";
    }
}


class DavyJones extends Entity implements Attacker {
    public DavyJones() {
        super();
    }

    public DavyJones(Position position) {
        super(position);
    }

    public List<Position> getAttackRange() {
        List<Position> attackRange = new ArrayList<>(8);
        for (int i = position.x - 1; i <= position.x + 1; ++i) {
            for (int j = position.y - 1; j <= position.y + 1; ++j)
                attackRange.add(new Position(i, j));
        }
        return attackRange;
    }

    @Override
    public boolean isAttacking(int x, int y) {
        for (Position position : getAttackRange())
            if (position.x == x && position.y == y)
                return true;
        return false;
    }

    @Override
    public String toString() {
        return "D";
    }
}


class Kraken extends Entity implements Attacker {
    public Kraken() {
        super();
    }

    public Kraken(Position position) {
        super(position);
    }

    @Override
    public boolean isAttacking(int x, int y) {
        return Math.abs(x - position.x) <= 1 && y == position.y
                || Math.abs(y - position.y) <= 1 && x == position.x;
    }

    @Override
    public String toString() {
        return "K";
    }
}


class Rock extends Entity implements Attacker {
    public Rock() {
        super();
    }

    public Rock(Position position) {
        super(position);
    }

    @Override
    public boolean isAttacking(int x, int y) {
        return position.x == x && position.y == y;
    }

    @Override
    public String toString() {
        return "R";
    }
}


class DeadMansChest extends Entity {
    public DeadMansChest() {
        super();
    }

    public DeadMansChest(Position position) {
        super(position);
    }

    @Override
    public String toString() {
        return "X";
    }
}


class Tortuga extends Entity {
    public Tortuga() {
        super();
    }

    public Tortuga(Position position) {
        super(position);
    }

    @Override
    public String toString() {
        return "T";
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

    public static int getHeuristicDistance(Position from, Position to) {
        return Math.max(Math.abs(from.x - to.x), Math.abs(from.y - to.y));
    }
}



class Position {
    /*
     * Defines a container for two values
     * Used in code to store positions of entities on a map
     * The fields x and y are public and have no getters and setters, because it is sufficient for just storing some values
     * */
    public Integer x;
    public Integer y;

    public Position() {

    }

    public Position(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Position)
            return ((Position) obj).x.equals(x) && ((Position) obj).y.equals(y);
        else
            return false;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}


class Path {
    private final List<Position> positions;
    private final boolean rumPicked;
    private boolean krakenKilled;

    public Path() {
        positions = new ArrayList<>();
        rumPicked = false;
        krakenKilled = false;
    }

    public Path(Path oldPath) {
        positions = new ArrayList<>(oldPath.getPositions());
        rumPicked = oldPath.isRumPicked();
        krakenKilled = oldPath.isKrakenKilled();
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void addPosition(Position position) {
        positions.add(position);
    }

    public void addPositionToBeginning(Position position) {
        positions.add(0, position);
    }

    public void extend(Path otherPath) {
        for (Position position : otherPath.positions)
            addPosition(position);
    }

    public boolean isRumPicked() {
        return rumPicked;
    }

    public boolean isKrakenKilled() {
        return krakenKilled;
    }

    public void setKrakenKilled(boolean killed) {
        krakenKilled = killed;
    }

    public int size() {
        return positions.size();
    }

    public Position getLastPosition() {
        return positions.get(positions.size() - 1);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for (Position pos : positions)
            res.append(pos.toString());
        return res.toString();
    }
}



class BinaryHeap {
    List<Integer> keys;
    List<Path> values;

    BinaryHeap() {
        keys = new ArrayList<>();
        values = new ArrayList<>();
    }

    private int getParentIndex(int i) {
        return i / 2;
    }

    private int getLeftChildIndex(int i) {
        return i * 2 + 1;
    }

    private int getRightChildIndex(int i) {
        return (i + 1) * 2;
    }

    private void heapify(int i) {
        int left = getLeftChildIndex(i);
        int right = getRightChildIndex(i);

        int min = i;

        if (left < keys.size() && keys.get(left) < keys.get(i))
            min = left;
        if (right < keys.size() && keys.get(right) < keys.get(min))
            min = right;

        if (min != i) {
            swapElements(keys, i, min);
            swapElements(values, i, min);
            heapify(min);
        }
    }

    public Path extractMin() {
        Path min = values.get(0);
        keys.set(0, keys.get(keys.size() - 1));
        keys.remove(keys.size() - 1);
        values.set(0, values.get(values.size() - 1));
        values.remove(values.size() - 1);
        heapify(0);
        return min;
    }

    private void increaseKey(int index, Integer key, Path value) {
        keys.set(index, key);
        values.set(index, value);
        while (index > 0 && keys.get(getParentIndex(index)) > keys.get(index)) {
            swapElements(keys, index, getParentIndex(index));
            swapElements(values, index, getParentIndex(index));
            index = getParentIndex(index);
        }
    }

    public void insertElement(Integer key, Path value) {
        keys.add(key);
        values.add(value);
        increaseKey(keys.size() - 1, key, value);
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }

    private static <T> void swapElements(List<T> list, int firstIndex, int secondIndex) {
        T temp = list.get(firstIndex);
        list.set(firstIndex, list.get(secondIndex));
        list.set(secondIndex, temp);
    }
}


class WrongInputException extends Exception {

}