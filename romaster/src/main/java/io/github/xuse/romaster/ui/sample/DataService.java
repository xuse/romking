package io.github.xuse.romaster.ui.sample;

import java.util.Arrays;
import java.util.List;

import io.github.xuse.simple.context.util.RandomData;

public class DataService {

	public static List<Person> getPeople() {
		return Arrays.asList(RandomData.newArrayInstance(Person.class, 100));
	}
	
}
