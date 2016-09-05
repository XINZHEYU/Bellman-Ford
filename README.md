# Bellman-Ford

Brief description

This project is a implementation of Bellman-Ford algorithm. It can let multiple clients who know their neighbors and cost to each neighbor to calculate the least cost to other clients in the whole topology. 

Instructions on how to run the code

Type make, and the .class file will appear in the same directory. 

Type "java bfclient <listen_port> <timeout> [<linkage_ip> <linkage_port> <linkage_name>]” to invoke a client application.
	
Command:
	linkdown <linkage_ip> <linkage_port>
	linkup <linkage_ip> <linkage_port>
	showrt
	close
	
The project will not be invoked if invoke input is invalid. However, invalid command is OK, you just need to input the correct command again.
