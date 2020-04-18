# Java 8 Default Methods
  - syntax - **default** void functionName(parameters) { ... }
  - provides default implementation in interface
  
## Rules 
- If "ClassMath" is implementing two interfaces "MathInterface" & "AnotherMathInterface", And both having same signature, then class "ClassMath" should provide implementation as multiple inheritance rule is broken. it will give compilation exception as "Duplicate default methods". So class can also call respective interface default method by using interface name with super as below. 
```
      	@Override
	public double divide(int a, int b) {
		return AnotherMathInterface.super.divide(a, b);
	}
```
- Rule 2
