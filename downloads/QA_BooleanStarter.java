/* Comments
Author: Quintin Ampofo
Instructor: Dr. Janett Walters-Williams
Class: CSC-204-01 Computer Architecture and Organization I
Description: This program parses, simplifies, and traces Boolean expressions using standard Boolean algebra rules. 
It applies simplification iteratively, step by step, until a fixed point is reached (with no further simplifications possible).
(At least, as best as I've learned so far. It isn't perfect, sadly.)
Date: 12-2-2025
*/

import java.util.Scanner;

public class QA_BooleanStarter{

    // ======== AST NODES ========
    interface Node {}

    static class Var implements Node {
        String name;
        Var(String n) { name = n; }
        public String toString() { return name; }
    }

    static class Const implements Node {
        boolean value;
        Const(boolean v) { value = v; }
        public String toString() { return value ? "1" : "0"; }
    }

    static class Not implements Node {
        Node x;
        Not(Node x) { this.x = x; }
        public String toString() {
            boolean simple = (x instanceof Var) || (x instanceof Const);
            return (simple ? x.toString() : "(" + x + ")") + "'";
        }
    }

    static class And implements Node {
        Node left, right;
        And(Node l, Node r) { left = l; right = r; }
        public String toString() {
            return left.toString() + "" + right.toString(); // implicit AND
        }
    }

    static class Or implements Node {
        Node left, right;
        Or(Node l, Node r) { left = l; right = r; }
        public String toString() {
            return left.toString() + " + " + right.toString();
        }
    }



    // ======== SIMPLE PARSER (char[] based) ========
    static class Parser {
        char[] s;
        int i = 0;

        Parser(String input) {
            s = input.replaceAll("\\s+", "").toCharArray();
        }

        boolean eof() { return i >= s.length; }
        char peek() { return s[i]; }
        char next() { return s[i++]; }

        Node parse() { return expr(); }

        // expr := term ('+' term)*
        Node expr() {
            Node left = term();
            while (!eof() && peek() == '+') {
                next();
                Node right = term();
                left = new Or(left, right);
            }
            return left;
        }

        // term := factor factor*   (implicit AND)
        Node term() {
            Node left = factor();
            while (!eof() &&
                  (isVar(peek()) || peek()=='(' || peek()=='0' || peek()=='1')) {
                Node right = factor();
                left = new And(left, right);
            }
            return left;
        }

        // factor := base ('\'' )*
        Node factor() {
            Node b = base();
            while (!eof() && peek()=='\'') {
                next();
                b = new Not(b);
            }
            return b;
        }

        // base := VAR | 0 | 1 | '(' expr ')'
        Node base() {
            if (eof()) throw new RuntimeException("Unexpected end of input!");
            char c = peek();

            if (isVar(c)) {
                next();
                return new Var("" + c);
            }

            if (c == '0') { next(); return new Const(false); }
            if (c == '1') { next(); return new Const(true); }

            if (c == '(') {
                next();
                Node e = expr();
                next(); // expect ')'
                return e;
            }

            throw new RuntimeException("Unexpected character: " + c);
        }

        boolean isVar(char c) {
            return c >= 'A' && c <= 'Z';
        }
    }
    
    // NodeList (with no java.util)
    static class NodeList{
      Node[] items;
      int size;
      
      NodeList(){
      items = new Node[8];
      size = 0;
      }
      void add(Node n){
         if (size == items.length){
            Node[] z = new Node[items.length * 2];
            for (int i = 0; i < items.length; i++)
               z[i] = items[i];
            items = z;
         }
         items[size++] = n;
       }
       
       Node get(int i){
         return items[i];
       }
       
       int size(){
         return size;
       }
       void removeAt(int x){
         for (int i = x; i < size-1; i++)
            items[i] = items[i+1];
            items[--size] = null; 
       }
       
       int indexOf(Node in){
         for (int i = 0; i < size; i++){
            if (items[i].equals(in))
               return i;
         }
         return -1;
       }
     } // end of NodeList class
     
     // Formatting for tracing!
     static int step = 1;
     static void emitTraceSingleLine(String ruleIdAndName, Node before, Node after, Node where){
      System.out.println(step + ". " + ruleIdAndName);
      System.out.println("    Before: " + before);
      System.out.println("    After: " + after);
      System.out.println("    Where: " + where);
      step++;
     } // end of method
     
     // Families attempted flags
     static boolean famIdentity, famNull, famComp, famIdemp, famInvol, famAb, famDeMor, famDist;
     
     // this method will apply one rule per step until it arrives at a fixed point
     static Node simplifyFull(Node root){
      while (true){
         // reset family flags
         famIdentity = famNull = famComp = famIdemp = famInvol = famAb = famDeMor = famDist;
         Result r = traverseAndApply(root);
         if (!r.changed){
         // no rule applied in traversal
            System.out.println("No further simplification possible -- reached fixed point.");
            System.out.print("Attempted families: ");
            boolean first = true;
            if (famIdentity){ 
               if (!first) 
                  System.out.print(", ");
               System.out.print("Identity");
               first = false;
            }
            if (famNull){ 
               if (!first) 
                  System.out.print(", ");
               System.out.print("Null");
               first = false;
            }
            if (famComp){ 
               if (!first) 
                  System.out.print(", ");
               System.out.print("Complement");
               first = false;
            }
            if (famIdemp){ 
               if (!first) 
                  System.out.print(", ");
               System.out.print("Idempotent");
               first = false;
            }
            if (famInvol){ 
               if (!first) 
                  System.out.print(", ");
               System.out.print("Involution");
               first = false;
            }
            if (famAb){ 
               if (!first) 
                  System.out.print(", ");
               System.out.print("Absorption");
               first = false;
            }
            if (famDeMor){ 
               if (!first) 
                  System.out.print(", ");
               System.out.print("DeMorgan");
               first = false;
            }
            if (famDist){ 
               if (!first) 
                  System.out.print(", ");
               System.out.print("Distributive");
               first = false;
            }
            
            System.out.println();
            return root;
         }
       root = r.node; // otherwise, update root and restart passes
      }  
    }
    
    // traversal result
    static class Result{
      boolean changed;
      Node node;
      
      Result(boolean c, Node n){
         changed = c;
         node = n;
      }
    }
    
    static Result traverseAndApply(Node n){
      if (n == null)
         return new Result(false, n);
      
      // Rules will be tested in required order. Flags are set to true before testing.
      
      //1: Identity
      famIdentity = true;
      Node after = applyIdentity(n);
      if(!equals(after, n)){
         emitTraceSingleLine("R1/4 Identity", n, after, n);
         return new Result(true, after);
      }
      
      //2: Null
      famNull = true;
      after = applyNull(n);
      if(!equals(after, n)){
         emitTraceSingleLine("R3 Null", n, after, n);
         return new Result(true, after);
      }
      
      //3: Complement
      famComp = true;
      after = applyComplement(n);
      if(!equals(after, n)){
         emitTraceSingleLine("R6/R8 Complement", n, after, n);
         return new Result(true, after);
      }
      
      //4: Idempotent
      famIdemp = true;
      after = applyIdempotent(n);
      if(!equals(after, n)){
         emitTraceSingleLine("R5/7 Idempotent", n, after, n);
         return new Result(true, after);
      }
      
      //5: Involution
      famInvol = true;
      after = applyInvolution(n);
      if(!equals(after, n)){
         emitTraceSingleLine("R9 Involution", n, after, n);
         return new Result(true, after);
      }
      
      //6: (Basic) Absorption 
      famAb = true;
      after = applyAbsorption(n);
      if(!equals(after, n)){
         emitTraceSingleLine("R10 Absorption", n, after, n);
         return new Result(true, after);
      }
      
      //6b: (Extended) Absorption 
      famAb = true;
      after = applyAbsorptionExtended(n);
      if(!equals(after, n)){
         emitTraceSingleLine("R11 Absorption-Extended", n, after, n);
         return new Result(true, after);
      }
      
      //DeMorgan's Law (DM1/2) 
      famDeMor = true;
      after = applyDeMorgan(n);
      if(!equals(after, n)){
         String id = ((n instanceof Not) && (((Not)n).x instanceof And)) ? "DM1 DeMorgan" : "DM2 DeMorgan";
         emitTraceSingleLine(id, n, after, n);
         return new Result(true, after);
      }
      
      // Distributive (reverse) 
      famDist = true;
      after = applyDistributive(n);
      if(!equals(after, n)){
         emitTraceSingleLine("R12 Distributive", n, after, n);
         return new Result(true, after);
      }
      
      // No rule matched at this node, so recurse (left then right)
      if (n instanceof And){
         And a = (And)n;
         Result leftRes = traverseAndApply(a.left);
         if (leftRes.changed)
            return new Result(true, new And(leftRes.node, a.right));
         Result rightRes = traverseAndApply(a.right);
         if (rightRes.changed)
            return new Result(true, new And(a.left, rightRes.node));
      }
      
      else if (n instanceof Or){
         Or o = (Or)n;
         Result leftRes = traverseAndApply(o.left);
         if (leftRes.changed)
            return new Result(true, new Or(leftRes.node, o.right));
         Result rightRes = traverseAndApply(o.right);
         if (rightRes.changed)
            return new Result(true, new Or(o.left, rightRes.node));
      }
      
      else if (n instanceof Not){
         Not no = (Not)n;
         Result innerRes = traverseAndApply(no.x);
         if (innerRes.changed)
            return new Result(true, new Not(innerRes.node));
      }
      
      return new Result(false, n);
   }
   
   // RULES!!!
   
   // Identity: X + 0 = X; X*1 = X
   static Node applyIdentity(Node n){
      if (n instanceof Or){
         NodeList parts = new NodeList();
         collectOr(n, parts);
         // if any 1 -> 1
         for (int i = 0; i < parts.size(); i++){
            if (parts.get(i) instanceof Const && ((Const)parts.get(i)).value)
               return new Const(true);  
         }
         // remove 0 parts
         NodeList kept = new NodeList();
         for (int i = 0; i < parts.size(); i++){
            Node p = parts.get(i);
            if (!(p instanceof Const && !((Const)p).value)) 
               kept.add(p);
         }
         if (kept.size() == 0)
            return new Const(false);
         if (kept.size() == 1)
            return kept.get(0);
         
         return buildOrFromList(kept);
      }
      if (n instanceof And){
         NodeList parts = new NodeList();
         collectAnd(n, parts);
         // if any 0 -> 0
         for (int i = 0; i < parts.size(); i++){
            if (parts.get(i) instanceof Const && !((Const)parts.get(i)).value)
               return new Const(false);  
         }
         // remove 1 parts
         NodeList kept = new NodeList();
         for (int i = 0; i < parts.size(); i++){
            Node p = parts.get(i);
            if (!(p instanceof Const && ((Const)p).value)) 
               kept.add(p);
         }
         if (kept.size() == 0)
            return new Const(true);
         if (kept.size() == 1)
            return kept.get(0);
         
         return buildAndFromList(kept);
      }
      return n;  
   }
   
   // Null: explicitly X + 1 = 1; X*0=0
   static Node applyNull(Node n){
      if (n instanceof Or){
         NodeList parts = new NodeList();
         collectOr(n, parts);
         for (int i = 0; i < parts.size(); i++){
            if (parts.get(i) instanceof Const && ((Const)parts.get(i)).value)
               return new Const(true);
         }
         return n;
      }
      if (n instanceof And){
         NodeList parts = new NodeList();
         collectAnd(n, parts);
         for (int i = 0; i < parts.size(); i++){
            if (parts.get(i) instanceof Const && !((Const)parts.get(i)).value)
               return new Const(false);
         }
         return n;
      } 
      return n;
   }
   
   // Complement: if Or contains X and X', then 1; if AND contains X and X', then 0
   static Node applyComplement(Node n){
      if(n instanceof Or){
         NodeList parts = new NodeList();
         collectOr(n, parts);
         for (int i = 0; i < parts.size(); i++){
            for (int j = 0; j < parts.size(); j++){
               if (i == j)
                  continue;
               Node p = parts.get(i), q = parts.get(j);
               if (p instanceof Not && equals(((Not)p).x, q))
                  return new Const(true); 
               if (q instanceof Not && equals(((Not)q).x, p))
                  return new Const(true);
            }
         }
         return n;
      }
      
      if(n instanceof And){
         NodeList parts = new NodeList();
         collectAnd(n, parts);
         for (int i = 0; i < parts.size(); i++){
            for (int j = 0; j < parts.size(); j++){
               if (i == j)
                  continue;
               Node p = parts.get(i), q = parts.get(j);
               if (p instanceof Not && equals(((Not)p).x, q))
                  return new Const(false); 
               if (q instanceof Not && equals(((Not)q).x, p))
                  return new Const(false);
            }
         }
         return n;
      }
      return n;
   }
   
   // Idempotent: remove duplicates in And/Or
   static Node applyIdempotent(Node n){
      if (n instanceof Or){
         NodeList parts = new NodeList();
         collectOr(n, parts);
         NodeList out = uniq(parts);
         if (out.size() == 1)
            return out.get(0);
         return buildOrFromList(out);
      }
      
      if (n instanceof And){
         NodeList parts = new NodeList();
         collectAnd(n, parts);
         NodeList out = uniq(parts);
         if (out.size() == 1)
            return out.get(0);
         return buildAndFromList(out);
      }
      return n;
   }
   
   // Involution: (X')' = X
   static Node applyInvolution(Node n){
      if (n instanceof Not && ((Not)n).x instanceof Not)
         return ((Not)((Not)n).x).x;
      return n;
   }
   
   // Basic Absorption: X + X*Y = X, and X*(X+Y) = X
   static Node applyAbsorption(Node n){
      if (n instanceof Or){
         NodeList parts = new NodeList();
         collectOr(n, parts);
         NodeList kept = new NodeList();
         for (int i = 0; i < parts.size(); i++)
            kept.add(parts.get(i));
         for (int i = 0; i < parts.size(); i++){
            Node t = parts.get(i);
            for (int j = 0; j < parts.size(); j++){
               Node p = parts.get(j);
               if (p instanceof And){
                  if (containsFactor((And)p, t)){
                     int idx = kept.indexOf(p);
                     if (idx >= 0)
                        kept.removeAt(idx);
                  }
               }
            } 
         }
         NodeList uniqKept = uniq(kept);
         if (uniqKept.size() == 1)
            return uniqKept.get(0);
         return buildOrFromList(uniqKept); 
      }
      
      if (n instanceof And){
         NodeList parts = new NodeList();
         collectAnd(n, parts);
         for (int i = 0; i < parts.size(); i++){
            Node p = parts.get(i);
            if (p instanceof Or){
               NodeList dis = new NodeList();
               collectOr(p, dis);
               for (int d = 0; d < dis.size(); d++){
                  Node cand = dis.get(d); // candidate for Absorption
                  boolean allHave = true;
                  for (int k = 0; k < parts.size(); k++){
                     if (k == i)
                        continue; 
                     if (!containsFactorNode(parts.get(k), cand)){
                        allHave = false;
                        break;
                     }
                  }
                  if (allHave)
                     return cand;
               }
            }
         }
         return n;
      }
      return n;
   }
   
   // Extended Absorption: X + X'Y = X + Y (done by replacing X'Y with Y when X is also present)
   static Node applyAbsorptionExtended(Node n){
      if (!(n instanceof Or))
         return n;
      NodeList parts = new NodeList(); 
      collectOr(n, parts);
      NodeList result = new NodeList();
      for (int i = 0; i < parts.size(); i++)
         result.add(parts.get(i));
      for (int i = 0; i < parts.size(); i++){
         Node atom = parts.get(i);
         for (int j = 0; j < parts.size(); j++){
            Node p = parts.get(j);
            if (p instanceof And){
               NodeList andFactors = new NodeList();
               collectAnd(p, andFactors);
               for (int f = 0; f < andFactors.size(); f++){
                  Node fac = andFactors.get(f);
                  if (fac instanceof Not && equals(((Not)fac).x, atom)){
                     NodeList rest = new NodeList();
                     for (int r = 0; r < andFactors.size(); r++){
                        if (r!=f)
                           rest.add(andFactors.get(r));
                     }
                     Node repl;
                     if (rest.size() == 0)
                        repl = new Const(true);
                     else if (rest.size() == 1)
                        repl = rest.get(0);
                     else
                        repl = buildAndFromList(rest);
                     int idx = result.indexOf(p);
                     if (idx >= 0){
                        result.removeAt(idx);
                        result.add(repl);
                     }
                  }
               }
            }
         }
      }
      
      for (int i = 0; i < result.size(); i++){
         if (result.get(i) instanceof Const && ((Const)result.get(i)).value)
            return new Const(true);
      }
      NodeList unique = uniq(result);
      if (unique.size() == 1)
         return unique.get(0);
      return buildOrFromList(unique);
   }
   
   // DeMorgan's Law!
   static Node applyDeMorgan(Node n){
      if (n instanceof Not && ((Not)n).x instanceof And){ // if applying to AND
         And a = (And)((Not)n).x;
         return new Or(new Not(a.left), new Not(a.right));
      }
      if (n instanceof Not && ((Not)n).x instanceof Or){ // if applying to OR
         Or o = (Or)((Not)n).x;
         return new And(new Not(o.left), new Not(o.right));
      }
      return n;
   }
   
   // Distributive (forward and reverse) (basically factoring)
   static Node applyDistributive(Node n) { // provided by AI and tweaked to fit specifications
    // OR node factoring: A*B + A*C => A*(B+C)
    if (n instanceof Or) {
        NodeList parts = new NodeList();
        collectOr(n, parts);

        for (int i = 0; i < parts.size(); i++) {
            Node p = parts.get(i);
            if (!(p instanceof And)) continue;
            NodeList A = new NodeList();
            collectAnd(p, A);

            for (int j = i + 1; j < parts.size(); j++) {
                Node q = parts.get(j);
                if (!(q instanceof And)) continue;
                NodeList B = new NodeList();
                collectAnd(q, B);

                // find common factor
                for (int a = 0; a < A.size(); a++) {
                    for (int b = 0; b < B.size(); b++) {
                        if (equals(A.get(a), B.get(b))) {
                            Node c = A.get(a);

                            // remaining factors
                            NodeList Arem = new NodeList();
                            for (int t = 0; t < A.size(); t++) if (t != a) Arem.add(A.get(t));
                            NodeList Brem = new NodeList();
                            for (int t = 0; t < B.size(); t++) if (t != b) Brem.add(B.get(t));

                            Node leftTerm = (Arem.size() == 0) ? new Const(true)
                                    : (Arem.size() == 1 ? Arem.get(0) : buildAndFromList(Arem));
                            Node rightTerm = (Brem.size() == 0) ? new Const(true)
                                    : (Brem.size() == 1 ? Brem.get(0) : buildAndFromList(Brem));

                            NodeList innerList = new NodeList();
                            innerList.add(leftTerm);
                            innerList.add(rightTerm);
                            Node inner = buildOrFromList(innerList);

                            Node factored = (inner instanceof Const && ((Const)inner).value) ? c : new And(c, inner);

                            NodeList newParts = new NodeList();
                            for (int k = 0; k < parts.size(); k++) if (k != i && k != j) newParts.add(parts.get(k));
                            newParts.add(factored);

                            NodeList uniqParts = uniq(newParts);
                            return (uniqParts.size() == 1) ? uniqParts.get(0) : buildOrFromList(uniqParts);
                        }
                    }
                }
            }
        }
    }

    // AND node factoring: (A+B)*(A+C) => A + (B*C)
    if (n instanceof And) {
        NodeList parts = new NodeList();
        collectAnd(n, parts);

        for (int i = 0; i < parts.size(); i++) {
            Node p = parts.get(i);
            if (!(p instanceof Or)) continue;
            NodeList A = new NodeList();
            collectOr(p, A);

            for (int j = i + 1; j < parts.size(); j++) {
                Node q = parts.get(j);
                if (!(q instanceof Or)) continue;
                NodeList B = new NodeList();
                collectOr(q, B);

                // find common term
                for (int a = 0; a < A.size(); a++) {
                    for (int b = 0; b < B.size(); b++) {
                        if (equals(A.get(a), B.get(b))) {
                            Node c = A.get(a);

                            NodeList Arem = new NodeList();
                            for (int t = 0; t < A.size(); t++) if (t != a) Arem.add(A.get(t));
                            NodeList Brem = new NodeList();
                            for (int t = 0; t < B.size(); t++) if (t != b) Brem.add(B.get(t));

                            Node leftTerm = (Arem.size() == 0) ? new Const(true)
                                    : (Arem.size() == 1 ? Arem.get(0) : buildOrFromList(Arem));
                            Node rightTerm = (Brem.size() == 0) ? new Const(true)
                                    : (Brem.size() == 1 ? Brem.get(0) : buildOrFromList(Brem));

                            Node inner = (leftTerm instanceof Const && ((Const)leftTerm).value) ? rightTerm
                                    : ((rightTerm instanceof Const && ((Const)rightTerm).value) ? leftTerm
                                    : new And(leftTerm, rightTerm));

                            Node result = (inner instanceof Const && ((Const)inner).value) ? c : new Or(c, inner);
                            return result;
                        }
                    }
                }
            }
        }
    }

    return n;
}
 
   
   // Efficiency Methods
   static void collectOr(Node n, NodeList out){
      if (n instanceof Or){
         collectOr(((Or)n).left, out);
         collectOr(((Or)n).right, out);
      }
      else
         out.add(n);
   }
   
   static void collectAnd(Node n, NodeList out){
      if (n instanceof And){
         collectAnd(((And)n).left, out);
         collectAnd(((And)n).right, out);
      }
      else
         out.add(n);
   }
   
   static Node buildOrFromList(NodeList l){
      if (l.size() == 0)
         return new Const(false);
      Node cur = l.get(0);
      for (int i = 1; i < l.size(); i++)
         cur = new Or(cur, l.get(i));
      return cur;
   }
   
   static Node buildAndFromList(NodeList l){
      if (l.size() == 0)
         return new Const(true);
      Node cur = l.get(0);
      for (int i = 1; i < l.size(); i++)
         cur = new And(cur, l.get(i));
      return cur;
   }
   
   static NodeList uniq(NodeList in){
      NodeList out = new NodeList();
      for (int i = 0; i < in.size(); i++){
         Node n = in.get(i);
         boolean found = false;
         for (int j = 0; j < out.size(); j++){
            if (equals(n, out.get(j))){
               found = true;
               break;
            }
         }
         if (!found)
            out.add(n);
      }
      return out;
   }
   
   static boolean containsFactor(And andNode, Node factor){
      NodeList facs = new NodeList();
      collectAnd(andNode, facs);
      for(int i = 0; i < facs.size(); i++){
         if (equals(facs.get(i), factor))
            return true;
      }
      return false;
   }
   
   static boolean containsFactorNode(Node n, Node factor){
      if (n instanceof And)
         return containsFactor((And)n, factor);
      return equals(n, factor);
   }
   
   static void removeFirstEquals(NodeList l, Node target){
      int idx = l.indexOf(target);
      if (idx >= 0)
         l.removeAt(idx);
   }
   
   static boolean equals(Node a, Node b){ // accounts for commutativity for and/or
      if (a == b)
         return true;
      if (a == null || b == null)
         return false;
      if (a.getClass() != b.getClass())
         return false;
      
      if (a instanceof Const)
         return ((Const)a).value == ((Const)b).value;
      if (a instanceof Var)
         return ((Var)a).name.equals(((Var)b).name);
      if (a instanceof Not) 
         return equals(((Not)a).x, ((Not)b).x);
         
      if (a instanceof And){
         NodeList lA = new NodeList(); // as in, "List A"
         collectAnd(a, lA);
         NodeList lB = new NodeList(); // as in, "List B"
         collectAnd(b, lB);
         if (lA.size() != lB.size())
            return false;
         boolean[] used = new boolean[lB.size()];
         for (int i = 0; i < lA.size(); i++){
            boolean found = false;
            for (int j = 0; j < lB.size(); j++){
               if (used[j])
                  continue;
               if (equals(lA.get(i), lB.get(j))){
                  used[j] = true;
                  found = true;
                  break;
               }
            }
            if (!found)
               return false;
         }
         return true;
      }
      
      if (a instanceof Or){
         NodeList lA = new NodeList(); // as in, "List A"
         collectOr(a, lA);
         NodeList lB = new NodeList(); // as in, "List B"
         collectOr(b, lB);
         if (lA.size() != lB.size())
            return false;
         boolean[] used = new boolean[lB.size()];
         for (int i = 0; i < lA.size(); i++){
            boolean found = false;
            for (int j = 0; j < lB.size(); j++){
               if (used[j])
                  continue;
               if (equals(lA.get(i), lB.get(j))){
                  used[j] = true;
                  found = true;
                  break;
               }
            }
            if (!found)
               return false;
         }
         return true;
      }
      
      return false; // if all else fails, do this 
   }

    // ======== DRIVER ========
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        System.out.print("Enter Boolean expression: ");
        String expr = in.nextLine();

        Parser p = new Parser(expr);
        Node root;
        try{ 
         root = p.parse();
        }
        
        catch(RuntimeException ex){
         System.out.println("Cannot simplify -- invalid syntax: " + ex.getMessage());
         return;
        }
        
        System.out.println("Input: " + root);
        System.out.println("Trace:");
        step = 1;
        Node finalRes = simplifyFull(root);
        System.out.println("Result: " + finalRes);
    }
}
