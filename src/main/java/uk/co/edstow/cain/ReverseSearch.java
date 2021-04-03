package uk.co.edstow.cain;

import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.Generator;
import uk.co.edstow.cain.pairgen.PairGen;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.structures.*;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.traversal.SOT;
import uk.co.edstow.cain.traversal.TraversalSystem;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;


public class ReverseSearch<G extends Goal<G>, T extends Transformation<R>, R extends Register> {

    @SuppressWarnings({"UnusedReturnValue", "unused"})
    public static class RunConfig<G extends Goal<G>, T extends Transformation<R>, R extends Register> {
        //interface
        private boolean liveCounter;
        private int livePrintPlans;
        private boolean quiet;

        // Search capabilities
        private int workers;

        // Search limits
        private int searchTime;
        private boolean timeOut;
        private long maxNodes;

        // Search Rules
        private Supplier<? extends TraversalSystem<WorkState<G,T,R>>> traversalAlgorithm;
        private int initialMaxDepth;
        private int forcedDepthReduction; // once a plan is found at maxDepth 10, cull searches at "maxDepth - forcedDepthReduction".
        private int initialMaxCost;
        private int forcedCostReduction;
        private Function<Plan<G,T,R>, Integer> costFunction;

        // Search Heuristics
        private int allowableSumTotalCoefficient; // cull plans that have higher totals than: "finalGoal * allowableSumTotalCoefficient + initialGoals".
        private int goalReductionsPerStep; // cull plans that have more active goals than steps before max maxDepth to reduce the number to one.
        private int goalReductionsTolerance; // add a tolerance to allow for a "less conservative" (lower) goalReductionsPerStep if larger reductions are unlikely.

        public RunConfig(boolean liveCounter, int livePrintPlans, boolean quiet, int workers, int searchTime, long maxNodes, boolean timeOut, Supplier<TraversalSystem<WorkState<G,T,R>>> traversalAlgorithm, Function<Plan<G, T, R>, Integer> costFunction, int initialMaxDepth, int forcedDepthReduction, int initialMaxCost, int forcedCostReduction, int allowableSumTotalCoefficient, int goalReductionsPerStep, int goalReductionsTolerance) {
            this.liveCounter = liveCounter;
            this.livePrintPlans = livePrintPlans;
            this.quiet = quiet;
            this.workers = workers;
            this.searchTime = searchTime;
            this.timeOut = timeOut;
            this.maxNodes = maxNodes;
            this.traversalAlgorithm = traversalAlgorithm;
            this.costFunction = costFunction;
            this.initialMaxDepth = initialMaxDepth;
            this.forcedDepthReduction = forcedDepthReduction;
            this.initialMaxCost = initialMaxCost;
            this.forcedCostReduction = forcedCostReduction;
//            this.registerAllocator=registerAllocator;
            this.allowableSumTotalCoefficient = allowableSumTotalCoefficient;
            this.goalReductionsPerStep = goalReductionsPerStep;
            this.goalReductionsTolerance = goalReductionsTolerance;
        }

        public RunConfig() {
            this.liveCounter = true;
            this.livePrintPlans = 2;
            this.quiet = false;
            this.workers = 1;
            this.searchTime = 60000;
            this.timeOut = false;
            this.maxNodes = 0;
            this.traversalAlgorithm = SOT.SOTFactory();
            this.costFunction = p -> p.maxCircuitDepth() + 200*p.depth();
            this.initialMaxDepth = 200;
            this.forcedDepthReduction = 1;
            this.initialMaxCost = Integer.MAX_VALUE;
            this.forcedCostReduction = 0;
            this.allowableSumTotalCoefficient = 2;
            this.goalReductionsPerStep = 1;
            this.goalReductionsTolerance = 1;
        }

        public RunConfig<G,T,R> setLiveCounter(boolean liveCounter){
            this.liveCounter = liveCounter;
            return this;
        }
        public RunConfig<G,T,R> setLivePrintPlans(int printPlans){
            this.livePrintPlans = printPlans;
            return this;
        }
        public RunConfig<G,T,R> setQuiet(boolean quiet){
            this.quiet = quiet;
            return this;
        }
        public RunConfig<G,T,R> setWorkers(int workers) {
            if(workers < 1){
                throw new IllegalArgumentException("workers must be greater than 0");
            }
            this.workers = workers;
            return this;
        }

        public RunConfig<G,T,R> setSearchTime(int searchTime) {
            if(searchTime < 0){
                throw new IllegalArgumentException("searchTime must be non-negative");
            }
            this.searchTime = searchTime;
            return this;
        }

        public RunConfig<G,T,R> setMaxNodes(int maxNodes) {
            if(maxNodes < 0){
                throw new IllegalArgumentException("maxNodes must be non-negative");
            }
            this.maxNodes = maxNodes;
            return this;
        }


        public RunConfig<G,T,R> setTimeOut(boolean timeOut) {
            this.timeOut = timeOut;
            return this;
        }

        public RunConfig<G,T,R> setTraversalAlgorithm(Supplier<? extends TraversalSystem<WorkState<G,T,R>>> traversalAlgorithm) {
            this.traversalAlgorithm = traversalAlgorithm;
            return this;
        }

        public RunConfig<G,T,R> setInitialMaxDepth(int initialMaxDepth) {
            if(initialMaxDepth < 0){
                throw new IllegalArgumentException("maxDepth must be non-negative");
            }
            this.initialMaxDepth = initialMaxDepth;
            return this;
        }

        public RunConfig<G,T,R> setForcedDepthReduction(int forcedDepthReduction) {
            this.forcedDepthReduction = forcedDepthReduction;
            return this;
        }

        public RunConfig<G,T,R> setInitialMaxCost(int initialMaxCost) {
            this.initialMaxCost = initialMaxCost;
            return this;
        }

        public RunConfig<G,T,R> setForcedCostReduction(int forcedCostReduction) {
            this.forcedCostReduction = forcedCostReduction;
            return this;
        }

        public RunConfig<G,T,R> setAllowableSumTotalCoefficient(int allowableSumTotalCoefficient) {
            if(allowableSumTotalCoefficient < 0){
                throw new IllegalArgumentException("allowableAtomsCoefficient must be non-negative");
            }
            this.allowableSumTotalCoefficient = allowableSumTotalCoefficient;
            return this;
        }

        public RunConfig<G,T,R> setGoalReductionsPerStep(int goalReductionsPerStep) {
            if(goalReductionsPerStep < 0){
                throw new IllegalArgumentException("goalReductionsPerStep must be non-negative");
            }
            this.goalReductionsPerStep = goalReductionsPerStep;
            return this;
        }

        public RunConfig<G,T,R> setGoalReductionsTolerance(int goalReductionsTolerance) {
            if(goalReductionsTolerance < 0){
                throw new IllegalArgumentException("goalReductionsTolerance must be non-negative");
            }
            this.goalReductionsTolerance = goalReductionsTolerance;
            return this;
        }
        public RunConfig<G,T,R> setCostFunction(Function<Plan<G,T,R>, Integer> costFunction) {
            this.costFunction = costFunction;
            return this;
        }
    }

//    private final int[] divisions;
    private final List<G> initialGoals;
    private final int initialGoalsAtomCount;
    private final int inputs;
    private final List<G> finalGoals;
    private final double finalGoalSumTotal;

    private final List<Plan<G,T,R>> plans;
    private final List<Long> planTimes;
    private final List<Long> planNodesExplored;
    private final ReentrantLock planLock;
    private final GoalsCache<G> cache;

    private final Generator<G,T,R> generator;

    private final int workers;
    private final AtomicBoolean end;
    private final Semaphore workersFinished;

    private long startTime;
    private final boolean liveCounter;
    private final int livePrintPlans;
    private final boolean quiet;

    private final long maxNodes;
    private final boolean timeout;
    private final long maxTime;
    private final AtomicInteger maxDepth;
    private final AtomicInteger maxCost;
    private final RegisterAllocator<G,T,R> registerAllocator;
//    private final int availableRegisters;
    private final Supplier<? extends TraversalSystem<WorkState<G,T,R>>> traversalAlgorithm;
    public final Function<Plan<G,T,R>, Integer> costFunction;

    private final int allowableSumTotalCoefficent;
    private final int forcedDepthReduction;
    private final int forcedCostReduction;
    private final int goalReductionsPerStep;
    private final int goalReductionsTolerance;

    private final Object workMonitor = new Object();

    public ReverseSearch(List<G> initialGoals, List<G> finalGoals, Generator<G,T,R> generator, RunConfig<G,T,R> runConfig, RegisterAllocator<G,T,R> registerAllocator) {
        this.liveCounter = runConfig.liveCounter;
        this.livePrintPlans = runConfig.livePrintPlans;
        this.quiet = runConfig.quiet;

        // Set up final and initial Goals
        this.finalGoals = Collections.unmodifiableList(new ArrayList<>(finalGoals));
        this.finalGoalSumTotal = new GoalBag<>(this.finalGoals).sumTotal();
        this.inputs = initialGoals.size();
        this.initialGoals = initialGoals;
        this.registerAllocator = registerAllocator;
        initialGoalsAtomCount = initialGoals.stream().mapToInt(g -> (int) g.total()).sum();


        // Init Internal components
        this.plans = new ArrayList<>();
        this.planTimes = new ArrayList<>();
        this.planNodesExplored = new ArrayList<>();
        this.planLock = new ReentrantLock();
        this.cache = new GoalsCache<>();


        // Init worker thread trackers and job distribution
        this.workers = runConfig.workers;
//        this.workQueue = new LinkedBlockingQueue<>();

        this.end = new AtomicBoolean(false);
        this.workersFinished = new Semaphore(1-this.workers);


        // Init search parameters
        this.traversalAlgorithm = runConfig.traversalAlgorithm;
        this.costFunction = runConfig.costFunction;
//        this.availableRegisters = runConfig.registerAllocator.getAvailableRegisters();
        this.maxDepth = new AtomicInteger(runConfig.initialMaxDepth);
        this.maxCost = new AtomicInteger(runConfig.initialMaxCost);
        this.maxTime = runConfig.searchTime;
        this.timeout = runConfig.timeOut;
        this.maxNodes = runConfig.maxNodes;
//        this.registerAllocator = runConfig.registerAllocator;

        // Init Heuristics
        this.allowableSumTotalCoefficent = runConfig.allowableSumTotalCoefficient;
        this.forcedDepthReduction = runConfig.forcedDepthReduction;
        this.forcedCostReduction = runConfig.forcedCostReduction;
        this.goalReductionsPerStep = runConfig.goalReductionsPerStep;
        this.goalReductionsTolerance = runConfig.goalReductionsTolerance;


        // Init PairGen with complete Object
        this.generator = generator;
//        this.pairGenFactory.init(initialGoals, finalGoals);

    }

    public List<G> getInitialGoals() {
        return initialGoals;
    }

    public List<G> getFinalGoals() {
        return finalGoals;
    }

    public List<Plan<G,T,R>> getPlans() {
        return plans;
    }

    public List<Long> getPlanTimes() { return planTimes; }

    public List<Long> getPlanNodesExplored() { return planNodesExplored; }


    private boolean running(){
        if(timeout && System.currentTimeMillis() > startTime + maxTime){
            return false;
        }
        return !end.get();
    }

    private void endTime(){
        end.set(true);
    }

    private String getWorkerInfo(List<Worker> workers){
        StringBuilder sb = new StringBuilder();
        sb.append("Active:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).active ? 'A' : 'W');
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | MinDepth:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).workerMinDepth);
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | MaxDepth:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).workerMaxDepth);
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | PlansFound:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).plansFound);
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | NodesExpanded:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).nodesExpanded);
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | CacheHits:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).cacheHits);
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | CacheChecks:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).cacheChecks);
            sb.append(i==workers.size()-1?']':',');
        }
        return sb.toString();
    }

    public void search(){
        startTime = System.currentTimeMillis();
        GoalBag<G> goals = new GoalBag<>(finalGoals);
        goals.setImmutable();
        List<Worker> workersThreads = new ArrayList<>();

        for (int i = 0; i < this.workers; i++) {
            Worker worker = new Worker(i);
            workersThreads.add(worker);
            if(i>0){
                worker.next = workersThreads.get(i-1);
            }
        }
        workersThreads.get(0).localTraversalSystem.add(new WorkState<>(0, goals, new Plan<>(finalGoals, initialGoals, generator.getDirectSolver().getDummyTransformation(Collections.emptyList(), finalGoals, new Context<>(-1, registerAllocator, initialGoals)))));
        workersThreads.get(0).next = workersThreads.get(workersThreads.size()-1);
        workersThreads.forEach(Thread::start);
        Thread mainThread = Thread.currentThread();

        if (!timeout && maxNodes==0){
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    System.out.println("\"exit\" to stop");
                    Scanner scan = new Scanner(System.in);
                    String input;
                    do {
                        input = scan.next();//nextLine();
                        System.out.println('"' + input + '"');
                        if(input.equals("r")){
                            for (Worker workersThread : workersThreads) {
                                workersThread.workerMinDepth = Integer.MAX_VALUE;
                                workersThread.workerMaxDepth = 0;
                            }
                        }

                    } while(!(input.equals("exit") || input.equals("e")));
                    System.out.println("Search ended by user");
                    endTime();
                    mainThread.interrupt();
                }
            }.start();
        }

        while (running()) {
            try {
                Thread.sleep(1000);
                if(liveCounter) {
                    System.out.print("\rRunning for: " + Duration.ofMillis(System.currentTimeMillis() - startTime).getSeconds() + " seconds"
                            + " | " + getWorkerInfo(workersThreads) + " | Cache Size: " + cache.size());
                }
                if (timeout && System.currentTimeMillis() > startTime + maxTime) {
                    if(!quiet) {
                        System.out.println("\nTime is up!");
                    }
                    endTime();
                }

                boolean allSleeping = workersThreads.stream().noneMatch(w -> w.active);
                if(workersFinished.tryAcquire() || allSleeping){
                    workersFinished.release();
                    if(!quiet) {
                        System.out.println("\nWorkers Are finished");
                    }

                    endTime();
                }
            } catch (InterruptedException e) {
                //ignore
                //System.out.println("Master interrupted");
            }
        }
        workersThreads.forEach(Thread::interrupt);
        try {
            workersFinished.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!quiet) {
            System.out.println("Master finished waiting");
        }

    }

    private class Worker extends Thread {
        final int id;
        boolean active = false;
        int workerMinDepth = 0;
        int workerMaxDepth = 0;
        private Worker next;

        final TraversalSystem<WorkState<G,T,R>> localTraversalSystem;
        int plansFound = 0;
        int cacheChecks = 0;
        int cacheHits = 0;
        int steals = 0;
        int nodesExpanded = 0;

        Worker(int id) {
            this.id = id;
            this.localTraversalSystem = traversalAlgorithm.get();

        }

        @Override
        public void run() {
            if (!quiet) {
                System.out.println("Worker " + id + " Starting");
            }
            try {
                while (running()) {
                    if(maxNodes>0 && nodesExpanded > maxNodes){
                        active = false;
                        break;
                    }
                    WorkState<G,T,R> s = localTraversalSystem.poll();
                    if (s == null) {
                        active = false;
                        if(workers>1) {
                            s = stealWork();
                        }else{
                            break;
                        }
                    }
                    active = true;
                    iSearch(s);
                }
            } catch (InterruptedException e) {
                //ignore
                //System.out.println(id + " interpreted!");
            }
            if (!quiet) {
                System.out.printf("Worker %d Finished:  Active: %b,  Min Depth: %d,  Max Depth: %d,  PlansFound: %d, CacheHits: %d,  CacheChecks: %d,  Steals: %d, NodesExpanded: %d%n", id, active, workerMinDepth, workerMaxDepth, plansFound, cacheHits, cacheChecks, steals, nodesExpanded);
            }
            workersFinished.release();
        }

        private WorkState<G,T,R> stealWork() throws InterruptedException {
            WorkState<G,T,R> s = null;
                while (true) {

                    for (Worker c = next; c.id != id && s == null; c = c.next) {
                        s = localTraversalSystem.steal(c.localTraversalSystem);
                    }

                    if (s != null)
                        break;

                    synchronized (workMonitor) {
                        try {
                            active = false;
                            workMonitor.wait();
                            active = true;
                        } catch (InterruptedException ignored) {}
                    }
                }

            steals++;
            return s;
        }

        private void iSearch(WorkState<G,T,R> s){
            int depth = s.depth;
            GoalBag<G> goals = s.goals;
            Plan<G,T,R> currentPlan = s.currentPlan;
            PairGen<G,T,R> goalPairs = s.pairGen;



            this.workerMinDepth = Math.min(this.workerMinDepth, depth);
            this.workerMaxDepth = Math.max(this.workerMaxDepth, depth);
            int currentMaxDepth = maxDepth.get();
            int currentMaxCost = maxCost.get();
            if (depth > currentMaxDepth) {
                return;
            }
            if (costFunction.apply(currentPlan) > currentMaxCost) {
                return;
            }

            if (goals.size() - (((currentMaxDepth - depth) * goalReductionsPerStep) + goalReductionsTolerance) > 1) {
                return;
            }

            if(goalPairs == null) {

                if (goals.sumTotal() >= ((allowableSumTotalCoefficent * finalGoalSumTotal) + initialGoalsAtomCount)) {
                    return;
                }

                cacheChecks++;
                if (!cache.isBest(goals, costFunction.apply(currentPlan))) {
                    cacheHits++;
                    return;
                }


                if (tryDirectSolve(depth, goals, currentPlan)) return;

                goalPairs = generator.getPairGenFactory().generatePairs(goals, new Context<>(depth, registerAllocator, initialGoals));
                nodesExpanded++;
            }
            int childNumber = goalPairs.getNumber();
            GoalPair<G,T, R> goalPair = goalPairs.next();

            if(goalPair == null){
                return;
            }

            GoalBag<G> newGoals = new GoalBag<>(goals);

            for (G upper : goalPair.getUppers()) {
                boolean removed = newGoals.removeEquivalent(upper);
                assert removed: "Pair Upper ERROR";
            }

            ArrayList<G> toAdd = new ArrayList<>();
            List<G> lowers = goalPair.getLowers();
            int[] interference = goalPair.getTransformation().inputRegisterIntraInterference();
            List<G> translation = new ArrayList<>(interference.length);

            for (int i = 0; i < lowers.size(); i++) {
                G l = lowers.get(i);

                boolean add = true;
                int c = i;
                while (c != interference[c]){
                    if(lowers.get(i).same(lowers.get(interference[c]))){
                        add = false;
                        break;
                    }
                    c = interference[c];
                }
                if(add) {
                    boolean addNew = true;
                    if(!goalPair.getTransformation().clobbersInput(i)) {
                        for (int j = 0; j < newGoals.size(); j++) {
                            G goal = newGoals.get(j);
                            if (goal.same(l)) {
                                newGoals.remove(j);
                                toAdd.add(goal);
                                translation.add(goal);
                                addNew = false;
                                break;
                            }
                        }
                    }
                    if(addNew) {
                        toAdd.add(l);
                        translation.add(l);
                    }
                } else {
                    translation.add(translation.get(interference[c]));
                }
            }
            newGoals.addAll(toAdd);
            newGoals.setImmutable();

            WorkState<G,T,R> child = null;
//            boolean tryChildren = newGoals.size() +(goalPair.getTransformation().ExtraRegisterCount()) <= availableRegisters;
            boolean tryChildren = registerAllocator.checkValid(newGoals, goalPair.getTransformation());
            if(tryChildren) {
                Plan<G,T,R> newPlan = currentPlan.newAdd(goalPair, newGoals, translation, childNumber);
                child = new WorkState<>(depth + 1, newGoals, newPlan, null);
            }
            WorkState<G,T,R> next = new WorkState<>(depth, goals, currentPlan, goalPairs);
            localTraversalSystem.add(child, next);
            synchronized (workMonitor) {
                workMonitor.notifyAll();
            }
        }

        private boolean tryDirectSolve(int depth, GoalBag<G> goals, Plan<G,T,R> currentPlan) {
            if (goals.size() > inputs) {
                return false;
            }
            if (goals.isEmpty() || goals.containsAll(initialGoals)) {
                if (addPlan(currentPlan, id, depth, this.nodesExpanded)) {
                    return true;
                }
            }
            List<GoalPair<G,T, R>> pairs = isTransformable(goals, depth);
            if (pairs != null && !pairs.isEmpty()) {
                Plan<G,T,R> p = currentPlan;
                GoalBag<G> currentGoals = new GoalBag<>(goals);

                for (int i = 0; i < pairs.size(); i++) {
                    GoalPair<G,T, R> pair = pairs.get(i);
                    for (G upper : pair.getUppers()) {
                        boolean removed = currentGoals.removeEquivalent(upper);
                        assert removed : "Pair Upper ERROR";
                    }
                    currentGoals.addAll(pair.getLowers());
                    List<G> translation = new ArrayList<>(pair.getLowers().size());
                    translation.addAll(pair.getLowers());
                    boolean tryChildren = registerAllocator.checkValid(currentGoals, pair.getTransformation());
                    if(!tryChildren){
                        return false;
                    }
                    p = p.newAdd(pair, currentGoals, translation, -1);
                }
                return addPlan(p, id, depth, this.nodesExpanded);
            }

            return false;
        }

        private void livePrintln(String str){
            if (livePrintPlans>0) System.out.println(str);
        }
        private void livePrintDebug(String str){
            if (livePrintPlans>1) System.out.println(str);
        }

        private boolean addPlan(Plan<G,T,R> p, int id, int depth, long nodesExpanded){
//            RegisterAllocator<G>.Mapping mapping = registerAllocator.solve(p, initialGoals);

            if (!registerAllocator.checkPossible(p)){
                livePrintln(this.id + " Plan Found but registers cannot be allocated");
                return false;
            }
            maxDepth.updateAndGet(x->Math.min(p.depth()-forcedDepthReduction, x));
            maxCost.updateAndGet(x->Math.min(costFunction.apply(p)-forcedCostReduction, x));
            Worker w = this;
            do {
                w.workerMinDepth = depth;
                w.workerMaxDepth = 0;
                w = w.next;
            } while(w != this);

            this.plansFound++;

            livePrintln("Found new Plan (id:" + id + "): Length: " + p.depth() + " (" + depth + ") | max-Circuit-Depth: " + p.maxCircuitDepth() + " | Cost: " + costFunction.apply(p));
            livePrintDebug(p.toString());

            try {
                planLock.lock();
                plans.add(p);
                planTimes.add(System.currentTimeMillis()-startTime);
                planNodesExplored.add(nodesExpanded);

                if (plans.size() > 1) {
                    Plan<G,T,R> lastPlan = plans.get(plans.size() - 2);
                    int i = 0;
                    while (i < p.getAll().size() && i < lastPlan.getAll().size() &&
                            lastPlan.getAll().get(i) == p.getAll().get(i)) {
                        i++;
                    }
                    livePrintDebug("Diverges from last plan at step: " + i);
                }
            } finally {
                planLock.unlock();
            }
            return true;
        }
    }


    private List<GoalPair<G,T, R>> isTransformable(GoalBag<G> goals, int depth) {
        return this.generator.getDirectSolver().solveDirectly(new Context<>(depth, registerAllocator, initialGoals), goals);
    }


    public void printStats(){
        if(!quiet) {
            System.out.println(cache);
            System.out.println("Number of Plans: " + plans.size());
            System.out.println("Current Max Depth: " + maxDepth);
            System.out.println("Current Max Cost: " + maxCost);
            try {
                planLock.lock();
                for (int i = 0; i < planTimes.size(); i++) {
                    System.out.println("Plan: " + i + " Cost: " + costFunction.apply(plans.get(i)) + " (length=" + plans.get(i).depth() + ") (circuit-maxDepth=" + Arrays.toString(plans.get(i).circuitDepths()) + ") found at " + planTimes.get(i) + "ms after locally searching " + planNodesExplored.get(i) + " nodes");
                }
            } finally {
                planLock.unlock();
            }
        }
    }




}
