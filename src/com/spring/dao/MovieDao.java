
package com.spring.dao;

import java.sql.Date;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutableTriple;

import com.spring.model.Movie;


public interface MovieDao {

	public List<Movie> getMovieList(int page, int n);

	public List<Movie> getMovieListWhereTitlesStartWith(String StartWith, String orderByColumn, String ascOrDesc, int page, int n);
	
	public int getTotalNumOfMovies();

	public List<Movie> getMovieListWithSearch(String title, int year, String director, String first_name,
			String last_name, String orderByColumn, String ascOrDesc, int page, int n);
	
	public List<Movie> getMovieListWithStar(int starID);
	
	public List<Movie> getMovieListWithGenre(String genre, String orderByColumn, String ascOrDesc, int page, int n); 
	
	public Movie getMovieListWithID(int id);
	
//	public String addMovieProcedure(String title, int year, String director,
//			String banner_url, String trailer_url, String starFN, String starLN, Date starDob,
//			String starPhotoURL, String genre);
	
	public List<Movie> fuzzy_search(String title); 
	public List<Movie> fuzzy_search_1(String title);
	public List<String> api_search(String title); 
	
	public List<Movie> getAllMovie();
	
	public List<String> getAllMovieTitle();
	
}
