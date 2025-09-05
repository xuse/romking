package io.github.xuse.romaster.ui.sample.obj;

import lombok.Data;

@Data
public  class Person {
	private String pictureUrl;
	private String fullName;
	private String profession;
	private String email;
	private Address address;
}