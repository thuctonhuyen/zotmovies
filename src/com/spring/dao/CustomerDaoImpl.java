package com.spring.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.spring.model.Customer;
import com.spring.model.Movie;
import com.spring.rowMapper.CustomerRowMapper;

public class CustomerDaoImpl implements CustomerDao {
	
	private DataSource dataSource;

	public CustomerDaoImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	@Override 
	public Boolean isCustomer(String email, String password)
	{
		try
		{
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			String sql = "SELECT * FROM customers WHERE email = ? and password = ?"; 
			Customer customer =  (Customer) jdbcTemplate.queryForObject(sql, new Object[] {email, password}, new CustomerRowMapper());
			
			return true; 
		}
		catch (EmptyResultDataAccessException e)
		{
			return false;
		}
	}
	
	@Override 
	public Customer getCustomer(int id)
	{
		try
		{
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			String sql = "SELECT * FROM customers WHERE id = ?";
			Customer customer =  (Customer) jdbcTemplate.queryForObject(sql, new CustomerRowMapper(), id);
			
			return customer; 
		}
		catch (EmptyResultDataAccessException e)
		{
			return null;
		}
		
	}
	
	public Customer getCustomerInfo(String email, String password)
	{
		try
		{
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			String sql = "SELECT * FROM customers WHERE email = ? and password = ?"; 
			Customer customer =  (Customer) jdbcTemplate.queryForObject(sql, new Object[] {email, password}, new CustomerRowMapper());
			
			return customer; 
		}
		catch (EmptyResultDataAccessException e)
		{
			return null;
		}
	}

}
