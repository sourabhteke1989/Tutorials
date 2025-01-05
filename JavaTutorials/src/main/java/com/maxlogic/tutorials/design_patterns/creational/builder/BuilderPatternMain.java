package com.maxlogic.tutorials.design_patterns.creational.builder;

public class BuilderPatternMain {

  /**
   * Builder pattern : Separate construction of complex object from its representation,
   *                 so that the same construction process can create different representations.
   *
   * Entities :
   *    Builder - Interface for building parts of product
   *    ConcreteBuilder - constructs and assembles parts of product by implementing Builder interface,
   *                      defines and keep track of parts,
   *                      provides interface for returning the final product build.
   *    Director - Uses Builder to construct Product,
   *               Client creates director object and initialize it with desired Builder object.
   *    Product - Product under construction.
   */
}
