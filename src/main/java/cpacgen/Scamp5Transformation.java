package cpacgen;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Scamp5Transformation extends Transformation{

    public enum Dir{
        North(0,1, "north"), East(1,0, "east"), South(0,-1, "south"), West(-1,0, "west");
        final int x;
        final int y;
        final String code;
        Dir(int x, int y, String code){
            this.x = x;
            this.y = y;
            this.code = code;
        }

        public String toCode() {
            return code;
        }
    }

    public class Res extends Scamp5Transformation{
        // u := {}

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("res(%s)", upper);
        }

        @Override
        public int inputCount() {
            return 0;
        }

        @Override
        public Goal applyForwards() {
            return new Goal();
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return "Res/1";
        }
    }

    public class Add_2 extends Scamp5Transformation {
        // u := a + b

        final Goal a;
        final Goal b;
        Goal sum;


        public Add_2(Goal a, Goal b) {
            this.a = a;
            this.b = b;
            this.sum = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("add(%s, %s, %s)", upper, lowers.get(0), lowers.get(1));
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            if (this.sum == null){
                this.sum = new Goal.Factory(a).addAll(b).get();
            }
            return this.sum;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Add2(%s, %s)", a, b);
        }
    }

    public class Add_3 extends Scamp5Transformation {
        // u := a + b + c

        final Goal a;
        final Goal b;
        final Goal c;
        Goal sum;


        public Add_3(Goal a, Goal b, Goal c) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.sum = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("add(%s, %s, %s, %s)", upper, lowers.get(0), lowers.get(1), lowers.get(2));
        }

        @Override
        public int inputCount() {
            return 3;
        }

        @Override
        public Goal applyForwards() {
            if (this.sum == null){
                this.sum = new Goal.Factory(a).addAll(b).addAll(c).get();
            }
            return this.sum;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Add3(%s, %s, %s)", a, b, c);
        }
    }

    public class Sub extends Scamp5Transformation {
        // u := a - b

         final Goal a;
         final Goal b;
         Goal difference;


         public Sub(Goal a, Goal b) {
             this.a = a;
             this.b = b;
             this.difference = null;
         }
         @Override
         public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
             assert lowers.size() == inputCount();
             return String.format("sub(%s, %s, %s)", upper, lowers.get(0), lowers.get(1));
         }

         @Override
         public int inputCount() {
             return 2;
         }

         @Override
         public Goal applyForwards() {
             if (this.difference == null){
                 this.difference = new Goal.Factory(a).subAll(b).get();
             }
             return this.difference;
         }

         @Override
         public double cost() {
             return 2;
         }

         @Override
         public String toStringN() {
             return String.format("Sub(%s, %s)", a, b);
         }
    }

    public class Neg extends Scamp5Transformation {
        // u := -a

        final Goal a;
        Goal neg;

        public Neg(Goal a) {
            this.a = a;
            this.neg = null;

        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("neg(%s, %s)", upper, lowers.get(0));
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards(){
            if(this.neg == null){
                this.neg = a.stream().map(Atom::negate).collect(Collectors.toCollection(Goal::new));
            }
            return this.neg;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Neg(%s)", a);
        }
    }

    public class Divq extends Scamp5Transformation {
        // u := a*0.5 + error

        final Goal a;
        Goal div;

        public Divq(Goal a) {
            this.a = a;
            this.div = null;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("divq(%s, %s)", upper, lowers.get(0));
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards() throws TransformationApplicationException {
            if(this.div == null){
                Goal.Factory factory = new Goal.Factory();
                if (!this.a.isEmpty()) {
                    int count = 1;
                    Atom last = a.get(0);
                    for (int i = 1; i < a.size()+1; i++) {
                        Atom c = i < a.size()?a.get(i):null;
                        if(!last.equals(c)){
                            if(count/2 != (count+1)/2){
                                throw new TransformationApplicationException("Cannot divide uneven number of atoms!");
                            } else {
                                for (int j = 0; j < count / 2; j++) {
                                    factory.add(last);
                                }
                            }
                            last = c;
                            count = 1;
                        } else {
                            count++;
                        }
                    }
                }
                this.div = factory.get();
            }
            return this.div;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Div(%s)", this.a);
        }
    }


    public class Movx extends Scamp5Transformation {
        //u := a_dir

        final Goal a;
        Goal moved = null;
        final Dir dir;

        public Movx(Goal a, Dir dir) {
            this.a = a;
            this.dir = dir;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("movx(%s, %s, %s)", upper, lowers.get(0), dir.toCode());
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards() {
            if(this.moved == null){
                Goal.Factory factory = new Goal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(-dir.x, -dir.y, 0)));
                this.moved = factory.get();
            }
            return this.moved;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("MovX %s (%s)", dir, this.a);
        }
    }


    public class Mov2x extends Scamp5Transformation {
        // u := a_dir1_dir2

        final Goal a;
        Goal moved = null;
        final Dir dir1;
        final Dir dir2;

        public Mov2x(Goal a, Dir dir1, Dir dir2) {
            this.a = a;
            this.dir1 = dir1;
            this.dir2 = dir2;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("mov2x(%s, %s, %s, %s)", upper, lowers.get(0), dir1.toCode(), dir2.toCode());
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards() {
            if(this.moved == null){
                Goal.Factory factory = new Goal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(-dir1.x -dir2.x, -dir1.y -dir2.y, 0)));
                this.moved = factory.get();
            }
            return this.moved;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("MovX %s %s (%s)", dir1, dir2, this.a);
        }
    }


    public class Addx extends Scamp5Transformation {
        // u := a_dir + b_dir

        final Goal a;
        final Goal b;
        Goal sum = null;
        final Dir dir;

        public Addx(Goal a, Goal b, Dir dir) {
            this.a = a;
            this.b = b;
            this.dir = dir;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("addx(%s, %s, %s, %s)", upper, lowers.get(0), lowers.get(1), dir.toCode());
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            if(this.sum == null){
                Goal.Factory factory = new Goal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(-dir.x, -dir.y, 0)));
                this.b.forEach(atom -> factory.add(atom.moved(-dir.x, -dir.y, 0)));
                this.sum = factory.get();
            }
            return this.sum;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Addx %s (%s, %s)", dir, this.a, this.b);
        }
    }


    public class Add2x extends Scamp5Transformation {
        // u := a_dir1_dir2 + b_dir1_dir2

        final Goal a;
        final Goal b;
        Goal sum = null;
        final Dir dir1;
        final Dir dir2;

        public Add2x(Goal a, Goal b, Dir dir1, Dir dir2) {
            this.a = a;
            this.b = b;
            this.dir1 = dir1;
            this.dir2 = dir2;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("add2x(%s, %s, %s, %s, %s)", upper, lowers.get(0), lowers.get(1), dir1.toCode(), dir2.toCode());
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            if(this.sum == null){
                Goal.Factory factory = new Goal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(-dir1.x-dir2.x, -dir1.y-dir2.y, 0)));
                this.b.forEach(atom -> factory.add(atom.moved(-dir1.x-dir2.x, -dir1.y-dir2.y, 0)));
                this.sum = factory.get();
            }
            return this.sum;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Add2x %s %s (%s, %s)", dir1, dir2, this.a, this.b);
        }
    }


    public class Subx extends Scamp5Transformation {
        // u := a_dir - b

        final Goal a;
        final Goal b;
        final Dir dir;
        Goal difference;


        public Subx(Goal a, Goal b, Dir dir) {
            this.a = a;
            this.b = b;
            this.dir = dir;
            this.difference = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("subx(%s, %s, %s, %s)", upper, lowers.get(0), dir, lowers.get(1));
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            if (this.difference == null){
                Goal.Factory factory = new Goal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(-dir.x, -dir.y, 0)));
                factory.subAll(b);
                this.difference = factory.get();
            }
            return this.difference;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("SubX %s (%s, %s)", dir, a, b);
        }
    }


    public class Sub2x extends Scamp5Transformation {
        // u := a_dir1_dir2 - b

        final Goal a;
        final Goal b;
        final Dir dir1;
        final Dir dir2;
        Goal difference;


        public Sub2x(Goal a, Goal b, Dir dir1, Dir dir2) {
            this.a = a;
            this.b = b;
            this.dir1 = dir1;
            this.dir2 = dir2;
            this.difference = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("subx(%s, %s, %s, %s, %s)", upper, lowers.get(0), dir1, dir2, lowers.get(1));
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            if (this.difference == null){
                Goal.Factory factory = new Goal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(-dir1.x-dir2.x, -dir1.y-dir2.y, 0)));
                factory.subAll(b);
                this.difference = factory.get();
            }
            return this.difference;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("SubX %s %s (%s, %s)", dir1, dir2, a, b);
        }
    }

}
