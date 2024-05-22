.class public Arithmetic_and
.super java/lang/Object

.method public static main([Ljava/lang/String;)V

	.limit stack 2
	.limit locals 2
	iconst_m1
	iconst_0
	iand
	istore_1
	iload_1

	ifne if1
	iconst_0
	invokestatic io/print(I)V
	goto endif1

if1:
	iconst_1
	invokestatic io/print(I)V

endif1:
	return
.end method

.method public <init>()V
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method