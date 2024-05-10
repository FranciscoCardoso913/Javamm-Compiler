.class public SimpleControlFlow
.super java/lang/Object

.method public static main([Ljava/lang/String;)V

	.limit stack 2
	.limit locals 4
	iconst_2
	istore_1
	iconst_3
	istore_2
	iload_2
	iload_1
	isub
	iflt cmp_lt_0_true
	iconst_0
	goto cmp_lt_0_end

cmp_lt_0_true:
	iconst_m1

cmp_lt_0_end:
	istore_3
	iload_3

	ifne if1
	iload_2
	invokestatic ioPlus/printResult(I)V
	goto endif1

if1:
	iload_1
	invokestatic ioPlus/printResult(I)V

endif1:
	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method