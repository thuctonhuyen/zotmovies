package com.spring.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jndi.JndiTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;

import com.spring.config.AppConfig;
import com.spring.config.MvcConfiguration;
import com.spring.dao.MovieDao;
import com.spring.model.Genre;
import com.spring.model.Genre_In_Movie;
import com.spring.model.Movie;
import com.spring.model.Star;

import javafx.util.Pair;

import java.sql.SQLException;
import java.text.ParseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import com.spring.model.Genre;

public class MovieParser_LoadFile extends DefaultHandler {
	// movies from database with key = (Director, Year, Title)
	private Hashtable<ImmutableTriple<String, Integer, String>, Movie> moviedb;

	// moviesFidXml is movies found from parsing xml and key = fid
	private Hashtable<String, Movie> moviesFidXml;

	// moviexml from parsing xml where key = (Director, Year, Title)
	private Hashtable<ImmutableTriple<String, Integer, String>, Movie> moviexml;

	// to read value of each tag
	private String tempVal;

	// to hold value of a movie after parsing
	private Movie tempMovie;

	// dataSource holds connection to database
	private DataSource dataSource;

	// boolean to indicate whether xml provides genre
	boolean hasCat;

	// new movie to add
	List<Movie> movieToAdd;

	// new genre_in_movie to add
	List<Pair<Genre, Movie>> genre_in_movieToAdd;

	// Hashtable of new genre to add:
	Hashtable<String, Genre> new_genres;

	// Hashtable of genre in db:
	Hashtable<String, Genre> genres_in_db;

	// Hashtable of genre in xml:
	Hashtable<String, ImmutableTriple<String, Integer, String>> genre_in_movie_db;

	// Connection of autocommit:
	Connection con;

	//
	List<Genre_In_Movie> genre_in_movie_toAdd;

	/*
	 * constructor for MovieParser() for: get data source to connect to db
	 * initialize moviesFidXml initialize moviedb to movies in database
	 * initialize moviexml
	 */
	public MovieParser_LoadFile() throws NamingException, SQLException {
		genre_in_movie_toAdd = new ArrayList<Genre_In_Movie>();
		movieToAdd = new ArrayList<Movie>();
		genre_in_movieToAdd = new ArrayList<Pair<Genre, Movie>>();

		System.out.println("Star logging for mains243.xml");

		ApplicationContext ctx = new ClassPathXmlApplicationContext("file:src/com/spring/config/dataSource_config.xml");
		dataSource = (DataSource) ctx.getBean("dataSource");

		con = dataSource.getConnection();
		con.setAutoCommit(false);

		moviesFidXml = new Hashtable<String, Movie>();

		moviedb = getMovieWithGenresInDb();
		// System.out.println(movieDao.getMovieHashtable());

		moviexml = new Hashtable<ImmutableTriple<String, Integer, String>, Movie>();

		hasCat = false;

		new_genres = new Hashtable<String, Genre>();

		genres_in_db = getGenresInDb();

	}

	// star parsing document here...
	private void parseDocument() {

		// get a factory
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {

			// get a new instance of parser
			SAXParser sp = spf.newSAXParser();

			// get the file:
			File xmlFile = new File("WebContent/resources/xml/mains243.xml");

			// make sure the encoding is ISO-8859-1
			InputStream inputStream = new FileInputStream(xmlFile);
			Reader reader = new InputStreamReader(inputStream, "ISO-8859-1");

			InputSource inputSource = new InputSource(reader);
			inputSource.setEncoding("ISO-8859-1");

			// parse the file and also register this class for call backs
			sp.parse(inputSource, this);

		} catch (SAXException se) {
			se.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}

	// Event Handlers
	// if encounter <film> tag, then a new movie will be created by default
	// constructor of Movie()
	/*
	 * default constructor of Movie: id = -1; year = 0000; listOfStars = new
	 * ArrayList<Star>(); directorId = -1; fid = "N/A"; listOfGenres = new
	 * ArrayList<Genre>();
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// reset:
		tempVal = "";
		if (qName.equalsIgnoreCase("film")) {
			tempMovie = new Movie();
			hasCat = false;
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		tempVal = new String(ch, start, length);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {

		// when encounter </film>, then update all the needed fields for a movie
		if (qName.equalsIgnoreCase("film")) {

			// validate data:
			boolean isValidFid = !tempMovie.getFid().equals("N/A"); // if not
																	// valid
																	// then get
																	// rid of
																	// tempMovie
			boolean isValidYear = tempMovie.getYear() != 0000; // if not valid
																// then reformat
																// to 0000

			// key is <director, year, title> => if string, then all are lower
			// case
			ImmutableTriple<String, Integer, String> key = new ImmutableTriple<String, Integer, String>(
					tempMovie.getDirector().toLowerCase(), tempMovie.getYear(), tempMovie.getTitle().toLowerCase());

			// duplicate hp when moviesFidXml already have that fid
			// or when moviedb already have that film
			// or when moviexml already have that film
			boolean isDuplicate = moviesFidXml.containsKey(tempMovie.getFid()) || moviedb.containsKey(key)
					|| moviexml.containsKey(key);

			// logging message to console
			if (!isValidFid) {
				System.out.println("Encounter invalid fid format " + tempMovie.getFid());
			}

			if (isDuplicate) {
				System.out.println("Encounter duplicate movie where title=" + tempMovie.getTitle() + " year = "
						+ tempMovie.getYear() + " director = " + tempMovie.getDirector());

			}

			// insert those valid
			if ((isValidFid && isValidYear && !isDuplicate)) {

				this.moviexml.put(key, tempMovie);
				this.moviesFidXml.put(tempMovie.getFid(), tempMovie);
				this.movieToAdd.add(tempMovie);

			}

			if (isDuplicate) {
				if (moviedb.containsKey(key)) {
					// check whether that movie has that genre or not:
					Movie m = moviedb.get(key);
					Hashtable<String, Genre> tempMovieGenre = new Hashtable<String, Genre>();
					for (Genre g : tempMovie.getGenres()) {
						tempMovieGenre.put(g.getName().toLowerCase(), g);
					}

					for (Genre g : m.getGenres()) {
						if (!tempMovieGenre.containsKey(g.getName().toLowerCase())) {
							m.addGenre(g);
							// add genre to list of movie
							moviedb.remove(key);
							moviedb.put(key, m);
						}
					}

				}
			}

		} else if (qName.equalsIgnoreCase("fid")) {
			tempMovie.setFid(tempVal.trim());

		} else if (qName.equalsIgnoreCase("t")) {
			tempMovie.setTitle(tempVal.trim());

		} else if (qName.equalsIgnoreCase("year")) {
			if (tryParseInt(tempVal.trim()))
				tempMovie.setYear(Integer.parseInt(tempVal.trim()));
			else {
				// invalid year format
				tempMovie.setYear(0000);
				System.out.println("Invalid year format '" + tempVal + "' changed to format 0000");

			}
		} else if (qName.equalsIgnoreCase("dirn")) {
			tempMovie.setDirector(tempVal.trim());
			// following => add list of genre for tempMovie
		} else if (qName.equalsIgnoreCase("cat")) {

			this.hasCat = true;

			String[] parts = tempVal.split(" ");

			for (int i = 0; i < parts.length; i++) {
				Genre genre = new Genre();
				genre.setName(parts[i].toLowerCase());

				if (!genre.getName().equals("N/A") && !genre.getName().isEmpty()
						&& !this.new_genres.containsKey(genre.getName().toLowerCase())
						&& !this.genres_in_db.containsKey(genre.getName().toLowerCase())) {
					this.new_genres.put(genre.getName().toLowerCase(), genre);
				}
				tempMovie.addGenre(genre);
			}

		} else if (qName.equalsIgnoreCase("cattext")) {
			String[] parts = tempVal.split(" ");
			
			
			if(hasCat)
			{
				tempMovie.setGenres(new ArrayList<Genre>());
				for (int i = 0; i < parts.length; i++) {
					Genre genre = new Genre();
					genre.setName(parts[i].toLowerCase());
	
					if (!genre.getName().equals("N/A") && !genre.getName().isEmpty()
							&& !this.new_genres.containsKey(genre.getName().toLowerCase())
							&& !this.genres_in_db.containsKey(genre.getName().toLowerCase())) {
						this.new_genres.put(genre.getName().toLowerCase(), genre);
					}
					tempMovie.addGenre(genre);
				}
			}
		}

	}

	public void PopulateGenre() throws SQLException {
		List<String> genreList = new ArrayList<String>();

		for (Entry<String, Genre> entry : this.new_genres.entrySet()) {
			genreList.add(entry.getValue().getName());

		}

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		String sql = "insert into genres (name) values (?)";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

			@Override
			public int getBatchSize() {
				return genreList.size();
			}

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setString(1, genreList.get(i));
			}

		});

		// dataSource.getConnection().commit();

	}

	public void PopulateMovie() throws SQLException {

		// this.movieToAdd
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql = "insert into movies (title, year, director) values (?, ?, ?)";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

			@Override
			public int getBatchSize() {
				return movieToAdd.size();
			}

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setString(1, movieToAdd.get(i).getTitle());
				ps.setInt(2, movieToAdd.get(i).getYear());
				ps.setString(3, movieToAdd.get(i).getDirector());
			}
		});

	}

	public void run() throws SQLException, NamingException {

		long startTime = System.nanoTime();
		System.out.println("Start parsing.... @" + startTime);
		parseDocument();

		System.out.println("Size of list of new movies in hashtable: " + this.moviexml.size());
		System.out.println("Size of list of movies to add " + this.movieToAdd.size());
		System.out.println("Size of list of new genres: " + this.new_genres.size());

		System.out.println("Start Populate Genre ... ");
		PopulateGenre();

		System.out.println("Start Populate Movie ... ");
		PopulateMovie();

		// combine 2 list:
		this.moviexml.putAll(this.moviedb);

		System.out.println("Total movies in db in hashtable form now: " + moviexml.size());
		Hashtable<Pair<Integer, Integer>, Integer> genres_in_movies_in_db = get_genre_in_movie_db();

		Hashtable<String, Genre> genres_in_db = getGenresInDb();
		Hashtable<ImmutableTriple<String, Integer, String>, Movie> movies_in_db = getMoviedb();
		
		Hashtable<Pair<Integer, Integer>, Integer> genre_in_movie_to_Add_hashtable = new Hashtable<Pair<Integer, Integer>, Integer>(); 

		for (Entry<ImmutableTriple<String, Integer, String>, Movie> entry : this.moviexml.entrySet()) {
			List<Genre> genres = entry.getValue().getGenres();
			List<Integer> genreIds = new ArrayList<Integer>();

			// System.out.println(genres);
			ImmutableTriple<String, Integer, String> mKey = entry.getKey();

			System.out.println(mKey);
			if (movies_in_db.containsKey(mKey)) {
				int movieId = movies_in_db.get(mKey).getId();

				if (!genres.isEmpty()) {
					for (Genre genre : genres) {
						if (!genre.getName().isEmpty()) {
							int genreId = (genres_in_db.get(genre.getName().toLowerCase()).getId());

							// look up id for movie:
							Pair<Integer, Integer> key = new Pair<Integer, Integer>(genreId, movieId);
							
							if (!genres_in_movies_in_db.containsKey(key) && !genre_in_movie_to_Add_hashtable.containsKey(key)) {
								
								Genre_In_Movie gim = new Genre_In_Movie();
								gim.setGenreId(genreId);
								gim.setMovieId(movieId);
								this.genre_in_movie_toAdd.add(gim);
								genre_in_movie_to_Add_hashtable.put(key, 1);
							}
						}
					}
				}
			}
		}

		System.out.println("Start Populate genres_in_movies ... ");
		PopulateGenreInMovie();

		long endTime = System.nanoTime();
		System.out.println("Finish mains.xml @" + endTime);
		System.out.println("Total: " + (endTime - startTime));
	}

	public void PopulateGenreInMovie() {

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql = "insert into genres_in_movies (genre_id, movie_id) values (?, ?)";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

			@Override
			public int getBatchSize() {
				return genre_in_movie_toAdd.size();
			}

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {

				ps.setInt(1, genre_in_movie_toAdd.get(i).getGenreId());
				ps.setInt(2, genre_in_movie_toAdd.get(i).getMovieId());

			}
		});

	}

	public Hashtable<ImmutableTriple<String, Integer, String>, Movie> getMovieWithGenresInDb() throws NamingException {

		Hashtable<ImmutableTriple<String, Integer, String>, Movie> hashtable = new Hashtable<ImmutableTriple<String, Integer, String>, Movie>();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "select genre_id, g.name, movie_id, m.director, m.year, m.title " + "from genres_in_movies gm "
				+ "join genres g on gm.genre_id = g.id " + "join movies m on gm.movie_id = m.id; ";

		List<Movie> listMovies = jdbcTemplate.query(query, new RowMapper<Movie>() {

			@Override
			public Movie mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
				Movie movie = new Movie();
				Genre genre = new Genre();

				genre.setId(resultSet.getInt(1));
				genre.setName(resultSet.getString(2));

				movie.setId(resultSet.getInt(3));
				movie.setDirector(resultSet.getString(4));
				movie.setYear(resultSet.getInt(5));
				movie.setTitle(resultSet.getString(6));
				movie.addGenre(genre);
				return movie;
			}

		});

		for (Movie movie : listMovies) {
			ImmutableTriple<String, Integer, String> key = new ImmutableTriple<String, Integer, String>(
					movie.getDirector().toLowerCase(), movie.getYear(), movie.getTitle().toLowerCase());
			boolean isEmpty = hashtable.isEmpty();
			boolean isDuplicate = hashtable.containsKey(key);

			if (isEmpty || !isDuplicate) {
				hashtable.put(key, movie);
			}

			if (isDuplicate) {
				List<Genre> tempGenre = hashtable.get(key).getGenres();
				List<Genre> currentGenre = movie.getGenres();

				// combine list of genres:
				tempGenre.addAll(currentGenre);
				Movie m = hashtable.get(key);
				m.setGenres(tempGenre);

				// update accordingly
				hashtable.remove(key);
				hashtable.put(key, m);
			}
		}

		// System.out.println(hashtable);
		return hashtable;

	}

	public static void main(String[] args) throws NamingException, SQLException {
		MovieParser spe = new MovieParser();
		spe.run();
	}

	private boolean tryParseInt(String number) {
		try {
			int temp = Integer.parseInt(number);
			return true;

		} catch (Exception e) {
			return false;
		}
	}

	public Hashtable<String, Movie> getMoviesHashTable() {
		return moviesFidXml;
	}

	public Hashtable<ImmutableTriple<String, Integer, String>, Movie> getMoviedbHashTable() {
		return this.moviedb;
	}

	public Hashtable<String, Genre> getGenresInDb() {
		Hashtable<String, Genre> hashtable = new Hashtable<String, Genre>();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "select * from genres";

		List<Genre> listGenres = jdbcTemplate.query(query, new RowMapper<Genre>() {

			@Override
			public Genre mapRow(ResultSet resultSet, int rowNumber) throws SQLException {

				Genre genre = new Genre();
				genre.setId(resultSet.getInt(1));
				genre.setName(resultSet.getString(2));
				return genre;
			}

		});

		for (Genre g : listGenres) {
			hashtable.put(g.getName().toLowerCase(), g);
		}

		// System.out.println(hashtable);
		return hashtable;

	}

	/*
	 * select genre_id, g.name, movie_id, m.director, m.year, m.title from
	 * genres_in_movies gm join genres g on gm.genre_id = g.id join movies m on
	 * gm.movie_id = m.id;
	 */
	public Hashtable<ImmutableTriple<String, Integer, String>, Movie> getMoviedb() throws NamingException {

		Hashtable<ImmutableTriple<String, Integer, String>, Movie> hashtable = new Hashtable<ImmutableTriple<String, Integer, String>, Movie>();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "select * from movies";

		List<Movie> listMovies = jdbcTemplate.query(query, new RowMapper<Movie>() {

			@Override
			public Movie mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
				Movie movie = new Movie();

				movie.setId(resultSet.getInt(1));
				movie.setTitle(resultSet.getString(2));
				movie.setYear(resultSet.getInt(3));
				movie.setDirector(resultSet.getString(4));

				return movie;
			}

		});

		for (Movie movie : listMovies) {
			ImmutableTriple<String, Integer, String> key = new ImmutableTriple<String, Integer, String>(
					movie.getDirector().toLowerCase(), movie.getYear(), movie.getTitle().toLowerCase());

			hashtable.put(key, movie);
		}

		// System.out.println(hashtable);
		return hashtable;

	}

	public Hashtable<Pair<Integer, Integer>, Integer> get_genre_in_movie_db() throws NamingException {
		Hashtable<Pair<Integer, Integer>, Integer> hashtable = new Hashtable<Pair<Integer, Integer>, Integer>();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "select * from genres_in_movies;";

		List<Genre_In_Movie> resultList = jdbcTemplate.query(query, new RowMapper<Genre_In_Movie>() {

			@Override
			public Genre_In_Movie mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
				Genre_In_Movie gim = new Genre_In_Movie();
				gim.setGenreId(resultSet.getInt(1));
				gim.setMovieId(resultSet.getInt(2));
				return gim;
			}

		});

		for (Genre_In_Movie gim : resultList) {
			Pair<Integer, Integer> key = new Pair<Integer, Integer>(gim.getGenreId(), gim.getMovieId());
			hashtable.put(key, 1);

		}

		// System.out.println(hashtable);
		return hashtable;

	}

}
