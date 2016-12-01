package de.appsist.service.usermodel.model;

import java.util.HashSet;
import java.util.Set;

public class EmployeeDevelopmentGoals
{
    // employees future position in company
    private String position;

    // set of contents employee should know
    private Set<String> contents;

    // set of items employee has to interact with either in current or future position
    private Set<String> items;

    public EmployeeDevelopmentGoals()
    {
        // init Sets
        this.contents = new HashSet<String>();
        this.items = new HashSet<String>();
    }

    // @return employees future position
    public String getPosition()
    {
        return position; 
    }

    // specify employees future position
    public void setPosition(String position)
    {
        this.position = position;
    }

    // @return Set of contents employee should read/learn
    public Set<String> getContents()
    {
        return contents;
    }

    // specify which contents employee should read/learn
    public void setContents(Set<String> contents)
    {
        this.contents = contents;
    }

    // add a content item the employee should read/learn
    public void addContent(String contentItem)
    {
        this.contents.add(contentItem);
    }

    // @return set of items the employee is interacting with at his/her current or future
    // position
    public Set<String> getItems()
    {
        return items;
    }

    // specify items the employee is interacting with at his/her current or future position
    public void setItems(Set<String> items)
    {
        this.items = items;
    }

    // add a content item the employee should read/learn
    public void addItem(String item)
    {
        this.items.add(item);
    }
}
