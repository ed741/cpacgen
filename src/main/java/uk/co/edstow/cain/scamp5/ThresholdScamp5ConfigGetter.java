package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.pairgen.PairGen;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

import java.util.List;

public class ThresholdScamp5ConfigGetter<G extends Goal<G>, T extends Transformation<R>, R extends Register, SELF extends Scamp5ConfigGetter.Scamp5Config<G, SELF>> implements Scamp5ConfigGetter<G,T,R, SELF> {
    private final CostHeuristic<G,T,R> heuristic;
    private final SELF scamp5Config;
    private final SELF scamp5ConfigMovOnly;
    private final int threshold;
    private final GenGetter<G,T,R, SELF> above;
    private final GenGetter<G,T,R, SELF> below;


    public ThresholdScamp5ConfigGetter(List<G> initialGoals, int threshold, CostHeuristic<G,T,R> heuristic, SELF scamp5Config, GenGetter<G, T, R, SELF> above, GenGetter<G, T, R, SELF> below) {
        this.heuristic = heuristic;
        this.scamp5Config = scamp5Config;
        this.scamp5ConfigMovOnly = scamp5Config.getMovOnlyVersion();
        this.threshold = threshold ;
        this.above = above;
        this.below = below;
    }


    @Override
    public PairGen<G,T,R> getScamp5Strategy(GoalBag<G> goals, Context<G,T,R> context, boolean movOnly) {
        int max = Integer.MIN_VALUE;
        for (G goal : goals) {
            max = (int) Math.max(max, goal.total());
        }
        PairGen<G,T,R> stratergy;
        if(max>threshold){
            stratergy = above.get(goals, context, movOnly? scamp5ConfigMovOnly : scamp5Config, heuristic);
        } else {
            stratergy = below.get(goals, context, movOnly? scamp5ConfigMovOnly : scamp5Config, heuristic);
        }
        return stratergy;
    }

    @Override
    public SELF getScamp5ConfigForDirectSolve(GoalBag<G> goals, Context<G,T,R> context) {
        return scamp5Config;
    }

    @FunctionalInterface
    public interface GenGetter<G extends Goal<G>, T extends Transformation<R>, R extends Register, C extends Scamp5ConfigGetter.Scamp5Config<G, C>>{
        PairGen<G,T,R> get(GoalBag<G> goals, Context<G,T,R> conf, C scamp5Config, CostHeuristic<G,T,R> heuristic);
    }
}