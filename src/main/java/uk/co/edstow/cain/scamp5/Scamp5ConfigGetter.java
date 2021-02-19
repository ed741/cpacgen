package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

public interface Scamp5ConfigGetter<G extends Goal<G>, T extends Transformation, C extends Scamp5ConfigGetter.Scamp5Config<G, C>> {
    PairGenFactory.PairGen<G, T> getScamp5Strategy(GoalBag<G> goals, Context<G, T> context, boolean movOnly);
    default PairGenFactory.PairGen<G, T> getScamp5Strategy(GoalBag<G> goals, Context<G, T> context){
        return getScamp5Strategy(goals, context, false);
    }
    C getScamp5ConfigForDirectSolve(GoalBag<G> goals, Context<G, T> context);

    interface Scamp5Config<G extends Goal<G>, SELF extends Scamp5Config<G, SELF>> {
        boolean onlyMov();
        SELF getMovOnlyVersion();
    }
}
