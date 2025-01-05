package com.maxlogic.tutorials.design_patterns.behavioral.visitor;

public class VisitorPatternMain {

  /**
   *  Visitor pattern : To visit structured object's, like tree, interconnected objects(car having parts), File system (Folders & files).
   *
   *  Example : Consider we have tree
   *
   *  CarElement Interface -> Car class, Wheel class, Body class, Engine class
   *
   *  CarElementVisitor Interface -> CarVisitor class, WheelVisitor class, BodyVisitor class, EngineVisitor class
   *
   *  Visitor Interface defines methods to visit each type of element.
   *  CarElement Interface only defines accept method, to accept visitor and call visit method for self,
   *  and call accept method for all other elements associated.
   *
   */

  public static void main(String[] args) {

  }

}
