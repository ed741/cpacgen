package uk.co.edstow.cain.scamp5.analogue;

import org.json.JSONObject;
import uk.co.edstow.cain.FileRun;
import uk.co.edstow.cain.RegisterAllocator;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.arrayGoal.ArrayGoal;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.scamp5.BasicScamp5ConfigGetter;
import uk.co.edstow.cain.scamp5.PatternHeuristic;
import uk.co.edstow.cain.scamp5.ThresholdScamp5ConfigGetter;

import java.util.List;

public abstract class Scamp5AnalogueFileRun<G extends Kernel3DGoal<G>> extends FileRun.Kernel3DFileRun<G> {


    public Scamp5AnalogueFileRun(JSONObject config) {
        super(config);
    }

    public Scamp5AnalogueFileRun(JSONObject config, List<G> finalGoals, int approximationDepth) {
        super(config, finalGoals, approximationDepth);
    }

    @Override
    protected PairGenFactory<G> makePairGenFactory(JSONObject json, RegisterAllocator<G> registerAllocator) {
        printLn("\t Making Pair Generation Factory:");
        printLn("Name                        : " + json.getString("name"));

        String configGetterName = json.getJSONObject("configGetter").getString("name");
        printLn("Config Getter               : " + configGetterName);
        switch (configGetterName) {
            default:
                throw new IllegalArgumentException("Unknown Scamp5 Scamp5ConfigGetter " + json.getString("configGetter"));
            case "Threshold":
                return getThresholdPairGenFactory(json.getJSONObject("configGetter"));
            case "Exhaustive":
                return getExhaustivePairGenFactory(json.getJSONObject("configGetter"));
            case "AtomDistanceSorted":
                return getAtomDistanceSortedPairGenFactory(json.getJSONObject("configGetter"));
            case "AtomDistance":
                return getAtomDistancePairGenFactory(json.getJSONObject("configGetter"));
        }

    }

    private Scamp5AnaloguePairGenFactory<G> getThresholdPairGenFactory(JSONObject json) {
        if(!json.has("threshold")) {throw new IllegalArgumentException("you need to define " + "threshold" + " inside configGetter");}
        int threshold = json.getInt("threshold");
        printLn("Exhaustive Search Threshold  : " + threshold);
        Scamp5AnalogueConfig<G> scampConfig = getScamp5Config(json, "ops");
        CostHeuristic<G> heuristic = getCostHeuristic(json, "heuristic");

        return new Scamp5AnaloguePairGenFactory<>(
                new ThresholdScamp5ConfigGetter<>(
                        initialGoals, threshold,
                        heuristic, scampConfig,
                        (goals, conf, scamp5Config, h) -> new Scamp5AnaloguePairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, scampConfig, heuristic),
                        (goals, conf, scamp5Config, h) -> new Scamp5AnaloguePairGenFactory.ExhaustivePairGen<>(goals, conf, scampConfig, heuristic)
                )
        );
    }

    private Scamp5AnaloguePairGenFactory<G> getExhaustivePairGenFactory(JSONObject json) {
        Scamp5AnalogueConfig<G> scampConfig = getScamp5Config(json, "ops");
        CostHeuristic<G> heuristic = getCostHeuristic(json, "heuristic");
        return new Scamp5AnaloguePairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5AnaloguePairGenFactory.ExhaustivePairGen<>(goals, conf, scamp5Config, heuristic)
        ));
    }
    private Scamp5AnaloguePairGenFactory<G> getAtomDistanceSortedPairGenFactory(JSONObject json) {
        Scamp5AnalogueConfig<G> scampConfig = getScamp5Config(json, "ops");
        CostHeuristic<G> heuristic = getCostHeuristic(json, "heuristic");
        return new Scamp5AnaloguePairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5AnaloguePairGenFactory.AtomDistanceSortedPairGen<>(goals, conf, scamp5Config, heuristic)
        ));
    }
    private Scamp5AnaloguePairGenFactory<G> getAtomDistancePairGenFactory(JSONObject json) {
        Scamp5AnalogueConfig<G> scampConfig = getScamp5Config(json, "ops");
        return new Scamp5AnaloguePairGenFactory<>(new BasicScamp5ConfigGetter<>(scampConfig,
                (goals, conf, scamp5Config) -> new Scamp5AnaloguePairGenFactory.AtomDistancePairGen<>(goals, conf, scamp5Config)
        ));
    }

    private Scamp5AnalogueConfig<G> getScamp5Config(JSONObject json, String name) {
        if(!json.has(name)) {throw new IllegalArgumentException("you need to define " + name + " inside configGetter");}
        printLn("Instruction to use          : " + json.getString("ops"));
        switch (json.getString(name)) {
            default:
                throw new IllegalArgumentException("Unknown Instructions option " + json.getString(name));
            case "all":
                return new Scamp5AnalogueConfig.Builder<G>().useAll().setSubPowerOf2(true).build();
            case "basic":
                return new Scamp5AnalogueConfig.Builder<G>().useBasic().setSubPowerOf2(true).build();
        }
    }

    private CostHeuristic<G> getCostHeuristic(JSONObject json, String name) {
        if(!json.has(name)) {throw new IllegalArgumentException("you need to define " + name + " inside configGetter");}
        printLn("CostHeuristic to use          : " + json.getString(name));
        switch (json.getString(name)) {
            default:
                throw new IllegalArgumentException("Unknown Heuristic option " + json.getString(name));
            case "Pattern":
                return new PatternHeuristic<>(initialGoals);
        }
    }




    public static class AtomGoalFileRun extends Scamp5AnalogueFileRun<AtomGoal> {
        public AtomGoalFileRun(JSONObject config) {
            super(config);
        }

        public AtomGoalFileRun(JSONObject config, List<AtomGoal> finalGoals, int approximationDepth) {
            super(config, finalGoals, approximationDepth);
        }

        @Override
        protected Kernel3DGoal.Goal3DAtomLikeFactory<AtomGoal> getGoalFactory() {
            return new AtomGoal.Factory();
        }
    }

    public static class ArrayGoalFileRun extends Scamp5AnalogueFileRun<ArrayGoal> {
        public ArrayGoalFileRun(JSONObject config) {
            super(config);
        }

        public ArrayGoalFileRun(JSONObject config, List<ArrayGoal> finalGoals, int approximationDepth) {
            super(config, finalGoals, approximationDepth);
        }

        @Override
        protected Kernel3DGoal.Goal3DAtomLikeFactory<ArrayGoal> getGoalFactory() {
            return new ArrayGoal.Factory();
        }
    }
}
