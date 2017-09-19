package ca.polymtl.inf4410.tp1.client;
import java.util.Arrays;

public class FakeServer {
	int execute(int a, int b) {
		return a + b;
	}
	int execute(byte[] b) {
		return b.length;
	}
}
