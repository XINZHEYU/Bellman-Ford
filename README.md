# Bellman-Ford

1. Brief description

This project is a implementation of Bellman-Ford algorithm. It can let multiple clients who know their neighbors and cost to each neighbor to calculate the least cost to other clients in the whole topology. 

The whole contains several classes, DistanceVector for create and update distance vector, Link saves the cost and hop to another node, which is used to generate distance vector, NeighborLink saves the distance vector and state of link of a neighbor, which can help to calculate distance vector, and bfclient for main process.

The main process has 4 threads, including listening thread, user interface thread, timer thread and main thread.

2. Details of development environment

I use eclipse to develop my project. And the JDK is Java 1.6.

3. Instructions on how to run the code

Type make, and the .class file will appear in the same directory. 

Type "java bfclient <listen_port> <timeout> [<linkage_ip> <linkage_port> <linkage_name>]” to invoke a client application.
	
Command:
	linkdown <linkage_ip> <linkage_port>
	linkup <linkage_ip> <linkage_port>
	showrt
	close
The project will not be invoked if invoke input is invalid. However, invalid command is OK, you just need to input the correct command again.

4. Bugs

Since I don’t implement the poison reverse mechanism, there will be some errors of least distance when you type linkdown or linkup. I will try to modify it before 23rd midnight and submit a modified version. Thank you.
