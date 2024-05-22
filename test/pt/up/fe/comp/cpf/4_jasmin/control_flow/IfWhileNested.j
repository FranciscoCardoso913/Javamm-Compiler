.class public IfWhileNested
.super java/lang/Object

.method public func(I)I

	.limit stack 3
	.limit locals 5
	iconst_m1
	istore_2
	iconst_0
	istore_3

whileCond1:
	iload_3
	iload_1
	isub
	iflt cmp_lt_0_true
	iconst_0
	goto cmp_lt_0_end

cmp_lt_0_true:
	iconst_m1

cmp_lt_0_end:
	istore 4
	iload 4
	ifne whileLoop1
	goto whileEnd1

whileLoop1:
	iload_2
	ifne if2
	iconst_2
	invokestatic ioPlus/printResult(I)V
	goto endif2

if2:
	iconst_1
	invokestatic ioPlus/printResult(I)V

endif2:
	iload_2
	iconst_m1
	ixor
	istore_2
	iinc 3 1
	goto whileCond1

whileEnd1:
	iconst_1
	ireturn
.end method

.method public static main([Ljava/lang/String;)V

	.limit stack 2
	.limit locals 3
	new IfWhileNested
	astore_1
	aload_1
	invokespecial IfWhileNested/<init>()V
	aload_1
	iconst_3
	invokevirtual IfWhileNested/func(I)I
	istore_2
	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method