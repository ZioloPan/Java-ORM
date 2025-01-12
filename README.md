## Java ORM Implementation

A lightweight Object-Relational Mapping (ORM) library in Java, designed to handle database operations with annotations for mapping classes to database tables. This project supports basic CRUD operations and simple relationship handling.

Design Patterns: 
- Object Pool:  
We created ConnectionPool class, which  stores queue of possible connections with database. Every thread can get his own connection and then release it after finished work   without generating new connection each time.
- Observer:  
  We created Observer interface and LoggerObserver implementation for observing changes in our database. ConnectionPool is a publisher, which notifies its subscribers about every change made also by other threads.
- Singleton  
  Our classes Config and ConnectionPool are singletons to ensure that only one instance is created. 
- Iterator  
  We implemented iterator as our own Collection with custom methods.
