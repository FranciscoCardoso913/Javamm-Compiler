.class public SimpleIfElseStat
.super java/lang/Object

.method public static main([Ljava/lang/String;)V

	.limit stack 3
	.limit locals 5
	iconst_5
	istore_1
	bipush 10
	istore_2
	iload_1
	iload_2
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
	bipush 10
	istore_1
	bipush 8
	istore_2
	iload_1
	iload_2
	isub
	iflt cmp_lt_1_true
	iconst_0
	goto cmp_lt_1_end

cmp_lt_1_true:
	iconst_m1

cmp_lt_1_end:
	istore 4
	iload 4
	ifne if2
	iload_2
	invokestatic ioPlus/printResult(I)V
	goto endif2

if2:
	iload_1
	invokestatic ioPlus/printResult(I)V

endif2:
	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method