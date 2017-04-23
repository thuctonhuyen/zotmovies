package com.spring.model;

public class Movie {
	int id;
	String title;
	int year;
	String director;
	String banner_url;
	String trailer_url;
	
	
	
	public Movie(int id, String title, int year, String director, String banner_url, String trailer_url) {
		super();
		this.id = id;
		this.title = title;
		this.year = year;
		this.director = director;
		this.banner_url = banner_url;
		this.trailer_url = trailer_url;
	}
	
	@Override
	public String toString() {
		return "Movie [id=" + id + ", title=" + title + ", year=" + year + ", director=" + director + ", banner_url="
				+ banner_url + ", trailer_url=" + trailer_url + "]";
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public String getDirector() {
		return director;
	}
	public void setDirector(String director) {
		this.director = director;
	}
	public String getBanner_url() {
		return banner_url;
	}
	public void setBanner_url(String banner_url) {
		this.banner_url = banner_url;
	}
	public String getTrailer_url() {
		return trailer_url;
	}
	public void setTrailer_url(String trailer_url) {
		this.trailer_url = trailer_url;
	}
	

}
