import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws WrongInputException {
//        EnvironmentMap map = new EnvironmentMap(true);
//        Decider decider = new Decider(map);
//        decider.findPath(true);
        new Tester(1000).runTests();
    }
}


class Tester {
    List<Double> executionTimesBacktracking;
    List<Double> executionTimesAStar;

    int testsAmount;
    int numberOfWinsBacktracking;
    int numberOfWinsAStar;

    public Tester(int testsAmount) {
        this.testsAmount = testsAmount;
        executionTimesBacktracking = new ArrayList<>(this.testsAmount);
        executionTimesAStar = new ArrayList<>(this.testsAmount);
        numberOfWinsBacktracking = 0;
        numberOfWinsAStar = 0;
    }

    public void runTests() {
        Position jackPosition, davyPosition, krakenPosition, rockPosition, chestPosition, tortugaPosition;
        boolean isInsideJack, isInsideDavy, isInsideKraken, isInsideRock, isInsideChest, underDavyAttack, underKrakenAttack;
        DavyJones davyInstance;
        Kraken krakenInstance;

        for (int i = 0; i < this.testsAmount; ++i) {
            System.out.println(i + "th test started");
            jackPosition = Position.getRandomPosition(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y);

            do
                davyPosition = Position.getRandomPosition(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y);
            while (davyPosition.equals(jackPosition));

            do
                krakenPosition = Position.getRandomPosition(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y);
            while (krakenPosition.equals(jackPosition) || krakenPosition.equals(davyPosition));

            do
                rockPosition = Position.getRandomPosition(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y);
            while (rockPosition.equals(jackPosition) || rockPosition.equals(davyPosition));

            davyInstance = new DavyJones(davyPosition);
            krakenInstance = new Kraken(krakenPosition);

            do {
                chestPosition = Position.getRandomPosition(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y);
                isInsideJack = jackPosition.equals(chestPosition);
                isInsideDavy = davyPosition.equals(chestPosition);
                isInsideKraken = krakenPosition.equals(chestPosition);
                isInsideRock = rockPosition.equals(chestPosition);
                underDavyAttack = davyInstance.isAttacking(chestPosition.x, chestPosition.y);
                underKrakenAttack = krakenInstance.isAttacking(chestPosition.x, chestPosition.y);
            } while (isInsideJack || isInsideDavy || isInsideKraken || isInsideRock || underDavyAttack || underKrakenAttack);

            do {
                tortugaPosition = Position.getRandomPosition(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y);
                isInsideDavy = davyPosition.equals(tortugaPosition);
                isInsideKraken = krakenPosition.equals(tortugaPosition);
                isInsideRock = rockPosition.equals(tortugaPosition);
                isInsideChest = chestPosition.equals(tortugaPosition);
                underDavyAttack = davyInstance.isAttacking(tortugaPosition.x, tortugaPosition.y);
                underKrakenAttack = krakenInstance.isAttacking(tortugaPosition.x, tortugaPosition.y);
            } while (isInsideDavy || isInsideKraken || isInsideRock || isInsideChest || underDavyAttack || underKrakenAttack);

            EnvironmentMap map = new EnvironmentMap(jackPosition, davyPosition, krakenPosition, rockPosition, chestPosition, tortugaPosition, Scenarios.SPYGLASS);
            Decider decider = new Decider(map);

            List<Decider.PathInformation> pathsInfo = decider.findPath(false);

            if (pathsInfo.get(0).win)
                ++numberOfWinsBacktracking;
            executionTimesBacktracking.add(pathsInfo.get(0).timeTaken);

            if (pathsInfo.get(1).win)
                ++numberOfWinsAStar;
            executionTimesAStar.add(pathsInfo.get(1).timeTaken);
        }

        double mean = Utils.getMean(executionTimesBacktracking);
        double mode = Utils.getMode(executionTimesBacktracking);
        double median = Utils.getMedian(executionTimesBacktracking);
        double standardDeviation = Utils.getStandardDeviation(executionTimesBacktracking);

        System.out.println("Backtracking:");
        System.out.println("mean: " + mean);
        System.out.println("mode: " + mode);
        System.out.println("median: " + median);
        System.out.println("standard deviation: " + standardDeviation);
        System.out.println("number of wins: " + numberOfWinsBacktracking);
        System.out.println("number of loses: " + (testsAmount - numberOfWinsBacktracking));

        mean = Utils.getMean(executionTimesAStar);
        mode = Utils.getMode(executionTimesAStar);
        median = Utils.getMedian(executionTimesAStar);
        standardDeviation = Utils.getStandardDeviation(executionTimesAStar);

        System.out.println("A*:");
        System.out.println("mean: " + mean);
        System.out.println("mode: " + mode);
        System.out.println("median: " + median);
        System.out.println("standard deviation: " + standardDeviation);
        System.out.println("number of wins: " + numberOfWinsAStar);
        System.out.println("number of loses: " + (testsAmount - numberOfWinsAStar));
    }
}


/**
 * A class used for path searching
 * Contains methods for finding paths with backtracking and A* algorithms
 */
class Decider {
    private final EnvironmentMap environmentMap;

    public Decider(EnvironmentMap environmentMap) {
        this.environmentMap = environmentMap;
    }

    /**
     * A container that holds information about algorithm evaluation
     */
    public static class PathInformation {
        public final boolean win;
        public final Path path;
        public final double timeTaken;

        public PathInformation(boolean win, Path path, double timeTaken) {
            this.win = win;
            this.path = path;
            this.timeTaken = timeTaken;
        }
    }

    /**
     * Finds path with backtracking and A* algorithms
     * @param outputFileNameBacktracking output filename for backtracking algorithm
     * @param outputFileNameAStar output filename for A* algorithm
     * @param saveToFile if true saves information to files
     * @return List of PathInformation instances with information about algorithms' results
     * @see PathInformation
     */
    public List<PathInformation> findPath(String outputFileNameBacktracking, String outputFileNameAStar, boolean saveToFile) {
        Path backtrackingResult = new Path(), aStarResult = new Path();
        long backtrackingStartTime = 0, aStarStartTime = 0;
        double backtrackingTimeTaken = 0.0, aStarTimeTaken = 0.0;
        switch (environmentMap.getScenario()) {
            case SUPER_SPYGLASS:
            case SPYGLASS:
                backtrackingStartTime = System.nanoTime();
                backtrackingResult = findPathSpyglassBacktrack();
                backtrackingTimeTaken = (double)(System.nanoTime() - backtrackingStartTime) / 1000000;
                aStarStartTime = System.nanoTime();
                aStarResult = findPathSpyglassAStarHead();
                aStarTimeTaken = (double)(System.nanoTime() - aStarStartTime) / 1000000;
        }

        boolean winBacktracking = backtrackingResult.size() != 0;
        PathInformation backtrackingInformation = new PathInformation(winBacktracking, backtrackingResult, backtrackingTimeTaken);
        if (saveToFile)
            printToFile(backtrackingInformation, outputFileNameBacktracking);

        boolean winAStar = aStarResult.size() != 0;
        PathInformation aStarInformation = new PathInformation(winAStar, aStarResult, aStarTimeTaken);
        if (saveToFile)
            printToFile(aStarInformation, outputFileNameAStar);

        return List.of(backtrackingInformation, aStarInformation);
    }

    public List<PathInformation> findPath() {
        return findPath("outputBacktracking.txt", "outputAStar.txt", true);
    }

    public List<PathInformation> findPath(boolean saveToFile) {
        return findPath("outputBacktracking.txt", "outputAStar.txt", saveToFile);
    }

    public void printToFile(PathInformation pathInformation, String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

            if (!pathInformation.win) {
                writer.write("Lose\n");
            } else {
                writer.write("Win\n");
                writer.append(String.valueOf(pathInformation.path.size())).append('\n');
                writer.append(pathInformation.path.toString()).append('\n');
                writer.append(environmentMap.getString(pathInformation.path));
                writer.append(String.valueOf((long)pathInformation.timeTaken)).append("ms\n");
            }
            writer.close();
        } catch (IOException exception) {
            System.out.println("Cannot open the file " + filename);
        }
    }

    /**
     * Finds the shortest path using backtracking algorithm.
     * Optimizations applied: uses minimal paths' lengths matrix, initially filled with -1. If cell is not reached, then matrix value for that cell equals -1.
     * First, it tries to find the shortest without considering killing the Kraken.
     * Second, it checks if Dead Man's Chest is reached. If yes, then it uses Decider.getPathAfterSearching to find the shortest path to the Chest.
     * If not, then it checks if Tortuga is reached. If it is reached, then it tries to find the shortest path to the Chest considering killing the Kraken.
     * If there is no path to Tortuga or there is no path from Tortuga to the Chest, the path does not exist.
     * Third, it returns the shortest path.
     * @return
     * @see Decider#findPathSpyglassBacktrackRecursion(Position, int, int[][], boolean, boolean)
     * @see Decider#getPathAfterSearching(Position, Position, int[][])
     * @see Path
     */
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

    /**
     * Recursively runs for all cells in Moore's neighborhood excluding cells that are out of map or dangerous.
     * It cuts off the directions where currentPathLength value is less to not consider the longer paths
     *
     * @param currentPosition the position from where we backtrack so far
     * @param currentPathLength the length of the path so far
     * @param pathLengthMatrix minimal paths' lengths matrix
     *
     */
    private void findPathSpyglassBacktrackRecursion(Position currentPosition, int currentPathLength, int[][] pathLengthMatrix, boolean rumPicked, boolean krakenKilled) {
        if (environmentMap.isDanger(currentPosition, rumPicked))
            return;
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

    /**
     * It uses minimal paths' lengths matrix obtained after algorithm to restore the shortest path
     * @param minLengthMatrix minimal paths' lengths matrix
     * @return the restored shortest path
     */
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

    /**
     * Finds the shortest path using A* algorithm
     * It uses findPathSpyglassAStarBody method the same way as findPathSpyglassBacktrack uses findPathSpyglassBacktrackRecursion
     * After findPathSpyglassAStarBody execution it runs getPathAfterSearching to restore the shortest path using pathLengths matrix
     * @return the shortest path
     * @see Decider#findPathSpyglassBacktrack()
     * @see Decider#findPathSpyglassBacktrackRecursion(Position, int, int[][], boolean, boolean)
     * @see Decider#findPathSpyglassAStarBody(Position, Position, int[][], boolean)
     * @see Path
     */
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
            pathLengths = Utils.createMatrix(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y, -1);
            findPathSpyglassAStarBody(jackPosition, tortugaPosition, pathLengths, false);
            pathObtained = getPathAfterSearching(jackPosition, tortugaPosition, pathLengths);
            pathLengths = Utils.createMatrix(EnvironmentMap.getSize().x, EnvironmentMap.getSize().y, -1);
            findPathSpyglassAStarBody(tortugaPosition, chestPosition, pathLengths, true);
            if (pathLengths[chestPosition.x][chestPosition.y] == -1)
                return new Path();
            pathObtained.extend(getPathAfterSearching(tortugaPosition, chestPosition, pathLengths));
        }

        return pathObtained;
    }

    /**
     * Finds the shortest path from position from to position to.
     * It uses pathLengths array as container to hold the cost of the path from start position
     */
    private void findPathSpyglassAStarBody(Position from, Position to, int[][] pathLengths, boolean rumPicked) {
        if (environmentMap.isDanger(from, rumPicked))
            return;
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
}


/**
 * Represents an initial map of the environment, contains all entities instances
 * @author Yakov Dementyev
 */
class EnvironmentMap {
    /**
     * Size of the map
     * @see Position
     */
    private static final Position size = new Position(9, 9);
    /**
     * List of all entities on the map
     * @see Entity
     */
    private List<Entity> entities;
    /**
     * List of all attacker entities
     * @see Attacker
     * @see Entity
     */
    private final List<Attacker> attackers;
    private JackSparrow jack;
    private DeadMansChest chest;
    private Tortuga tortuga;
    private Kraken kraken;
    private Scenarios scenario;

    /**
     * Private constructor, that initializes entities and attackers lists
     */
    private EnvironmentMap() {
        entities = new ArrayList<>();
        attackers = new ArrayList<>();
    }

    /**
     * Constructor, that can be used to initialize EnvironmentMap if all positions are known
     * @param jackPosition Jack Sparrow's position
     * @param davyPosition Davy Jones' position
     * @param krakenPosition Kraken's position
     * @param rockPosition Rock's position
     * @param chestPosition Dead Man's chest's position
     * @param tortugaPosition Tortuga position
     * @param scenario chosen scenario
     * @see Position
     * @see Scenarios
     */
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

    /**
     * Constructor that initializes EnvironmentMap if data is in stdin or in file.
     * @param readFromFile if true, then reads from file, else reads from stdin
     * @throws WrongInputException there could be some error data in file or in stdin, so the map cannot be initialized
     * @see EnvironmentMap#readDataFromFile(String filename)
     * @see EnvironmentMap#readDataFromConsole()
     */
    public EnvironmentMap(boolean readFromFile) throws WrongInputException {
        this();
        if (readFromFile)
            readDataFromFile("input.txt");
        else
            readDataFromConsole();
    }

    /**
     * Method called from constructor if data is given in file. Calls handleInputData after execution
     * @param filename Name of the file with information about entities positions
     * @throws WrongInputException there could be some error data in file, so the map cannot be initialized
     * @see EnvironmentMap#handleInputData(String firstLine, String secondLine)
     */
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

    /**
     * Method called from constructor if data is given in stdin. Calls handleInputData after execution
     * @throws WrongInputException there could be some error data in stdin, so the map cannot be initialized
     * @see EnvironmentMap#handleInputData(String firstLine, String secondLine)
     */
    private void readDataFromConsole() throws WrongInputException {
        String firstLine;
        String secondLine;

        Scanner scanner = new Scanner(System.in);

        firstLine = scanner.nextLine();
        secondLine = scanner.nextLine();

        scanner.close();

        handleInputData(firstLine, secondLine);
    }

    /**
     * Method for parsing data from file or stdin. Fills entities and attackers lists.
     *
     * @param firstLine a string, where there are all information about entities' positions
     * @param secondLine a string with scenario number
     * @throws WrongInputException throws when the arguments are not parsable
     */
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

    /**
     *
     * @return Position instance that contains width and height of the map
     * @see Position
     */
    public static Position getSize() {
        return size;
    }

    /**
     * Checks if given cell is under attack
     * @param x first coordinate of the cell
     * @param y second coordinate of the cell
     * @param krakenKilled the method consider if the kraken is killed or not
     * @return true if cell is attacked and false if not
     */
    public boolean isDanger(int x, int y, boolean krakenKilled) {
        for (Attacker attacker : attackers)
            if (attacker.isAttacking(x, y) && !(attacker instanceof Kraken && krakenKilled))
                return true;
        return false;
    }

    /**
     * A wrap for EnvironmentMap.isDanger(int x, int y, boolean krakenKilled) with Position instance instead of two ints
     * @param position a position of the cell to check
     * @param rumPicked the method consider if the kraken is killed or not
     * @return true if cell is attacked and false if not
     * @see EnvironmentMap#isDanger(int x, int y, boolean krakenKilled)
     * @see Position
     */
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

    /**
     * @return String description of the map without Path
     * @see Path
     */
    public String getString() {
        return getString(new Path());
    }

    /**
     *
     * @param path put path on a map
     * @return String description of the map with Path on it
     * @see Path
     */
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

    /**
     * checks if given coordinates are on map
     * @param x first coordinate
     * @param y second coordinate
     * @return true if contains, false if not
     */
    public static boolean isOnMap(int x, int y) {
        return x >= 0 && x < size.x && y >= 0 && y < size.y;
    }

    /**
     * wrap for EnvironmentMap.isOnMap(int x, int y)
     * @param position position of the cell
     * @see EnvironmentMap#isOnMap(int x, int y)
     * @see Position
     */
    public static boolean isOnMap(Position position) {
        return isOnMap(position.x, position.y);
    }
}


/**
 * Abstract class for each entity on the map. Contains methods for positioning
 */
abstract class Entity {
    /**
     * Entity's position on the map
     */
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


/**
 * Interface for attacking entities
 */
interface Attacker {
    boolean isAttacking(int x, int y);
}


/**
 * Represents Jack Sparrow on the map
 */
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


/**
 * Represents Davy Jones on the map, implements Attacker
 * @see Attacker
 */
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


/**
 * Represents Kraken on the map, implements Attacker
 * @see Attacker
 */
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


/**
 * Represents Rock on the map, implements Attacker
 * @see Attacker
 */
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

/**
 * Represents Dead Man's Chest on the map
 * @see Attacker
 */
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


/**
 * Represents Tortuga on the map
 * @see Attacker
 */
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


/**
 * Class that contains supporting static methods
 */
class Utils {
    /**
     * initializes two-dimensional array with given default value
     */
    public static int[][] createMatrix(int sizeX, int sizeY, int defaultValue) {
        int[][] matrix = new int[sizeX][sizeY];

        for (int i = 0; i < sizeX; ++i)
            for (int j = 0; j < sizeY; ++j)
                matrix[i][j] = defaultValue;

        return matrix;
    }

    /**
     * Calculates heuristic distance between two positions
     * @param from from where to start
     * @param to target position
     * @return heuristic distance between given positions
     * @see Position
     */
    public static int getHeuristicDistance(Position from, Position to) {
        return Math.max(Math.abs(from.x - to.x), Math.abs(from.y - to.y));
    }

    /**
     * Calculates mean value for given list of numbers
     */
    public static double getMean(List<Double> numbers) {
        if (numbers.size() == 0)
            return 0.0;
        return numbers.stream().reduce(Double::sum).orElse(0.0) / numbers.size();
    }

    /**
     * Calculates mode value for given list of numbers
     */
    public static double getMode(List<Double> numbers) {
        if (numbers.size() == 0)
            return 0.0;
        Map<Double, Integer> count = new HashMap<>();

        for (Double num : numbers) {
            if (count.containsKey(num))
                count.put(num, count.get(num) + 1);
            else
                count.put(num, 1);
        }

        return count.entrySet().stream().max(Comparator.comparing(Map.Entry::getValue)).get().getKey();
    }

    /**
     * Calculates median value for given list of numbers
     */
    public static double getMedian(List<Double> numbers) {
        if (numbers.size() == 0)
            return 0.0;

        List<Double> sorted = numbers.stream().sorted().collect(Collectors.toList());
        if (numbers.size() % 2 == 0)
            return (sorted.get(sorted.size() / 2) + sorted.get(sorted.size() / 2 + 1)) / 2;
        else
            return sorted.get(sorted.size() / 2);
    }


    /**
     * Calculates standard deviation for given list of numbers
     */
    public static double getStandardDeviation(List<Double> numbers) {
        if (numbers.size() == 0)
            return 0.0;

        double mean = getMean(numbers);

        return Math.sqrt(numbers.stream().map(number -> Math.pow(number - mean, 2)).reduce(Double::sum).orElse(0.0));
    }
}


/**
 * Enumeration used to hold scenario case
 */
enum Scenarios {
    SPYGLASS,
    SUPER_SPYGLASS
}


/**
 * Defines a container for two integers
 * Used in code to store positions of entities on a map
 * The fields x and y are public and have no getters and setters, because it is sufficient for just storing some values
 * */
class Position {
    public Integer x;
    public Integer y;

    /**
     * Initializes the Random instance for Position.getRandomPosition(int boundX, int boundY)
     */
    private static final Random random = new Random(System.nanoTime());

    public Position() {

    }

    public Position(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return random position with given boundaries
     */
    public static Position getRandomPosition(int boundX, int boundY) {
        return new Position(random.nextInt(boundX), random.nextInt(boundY));
    }

    /**
     * checks for equality of two positions
     * @return true if x and y of each position are equal
     */
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


/**
 * A container for positions, also considers if Tortuga is visited and if Kraken is killed
 */
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


/**
 * Implementation of priority queue used for A* algorithm
 */
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