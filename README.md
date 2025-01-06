This project was created for the class "Multi-Agent Systems" for the Master's in Software Engineering at the Faculty of Sciences at the University of Lisbon.

It consists on recreating the game "Among Us" with JADE (Java Agent Development Framework), which is a Java framework for the development of software agents. Different agents were created for each role of the original game (ex.: CREWMATE, IMPOSTER, REACTOR, OXYGEN, LIGHTS)
creating a multi-agent system that interacted and played by itself.

This was a group project done by Marta Correia, Francisco Cavaco and Miguel Tavares. The report is attached, althought in European Portuguese.

How to run the project on Eclipse:
1.  Add the project to Eclipse;
2.  Add jade.jar as a library and the run configurations;
3.  Add jade.Boot as the main class;
4.  Add _-port <port> game:Game(<numOfPlayers>,<numOfImposters>)_ as the running argument, where
   port is the desired local port, numOfPlayers is the number of crewmates (between 4 and 10, advised 10), and numOfImposters is the number of imposters (between 1 and 3, advised 2).
