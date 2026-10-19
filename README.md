<div align="center">
  
# Real Estate Management System

</div>
The project is a web application for a real estate agency. It helps agency staff register properties, create listings, organize property viewings, manage clients, negotiate offers, prepare contracts, and track agency commissions. The application is used by the secretary, real estate agents, and the agency manager. Property owners and potential buyers or tenants communicate with the agency staff; they do not have accounts in the application.

The project has three main areas:
* Listing and Property Management - properties, listings, the zones of the agents and the assignment of listings to agents;
* Client and Viewing Management - clients, their requests, the interest in a listing and the viewings;
* Transaction and Contract Management - price negotiation, contracts, payments and commissions.

# Contents
* [Initial Backlog](#initial-backlog)
* [ADR 001 - Technology Stack Selection](#technology-stack-selection)
* [C4 Context Diagram](#c4-context-diagram)
* [Use Case Diagrams](#use-case-diagrams)
* [Class Diagram](#class-diagram)
* [Database Schema](#database-schema)
* [Our Team](#our-team)

# Initial Backlog


# Technology stack selection


# C4 Context Diagram


# Use Case Diagrams


# Class Diagram


# Database Schema

The database was made starting from the class diagram. Each class became a table, and the associations between classes became foreign keys. Agent shares its primary key with Persoana, because an agent is also a person. The tables AgentZona, CerereZona and AgentRezultat are used for the many-to-many relationships: the zones of an agent, the preferred zones of a request, and the agents who take part in a transaction. The states and types are saved as text, with the same values as in the class diagram.

![Database Schema](Diagrams/BD_schema.png)

# Our Team


