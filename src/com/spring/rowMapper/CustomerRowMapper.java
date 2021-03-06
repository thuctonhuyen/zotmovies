package com.spring.rowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.spring.model.Customer;

public class CustomerRowMapper implements RowMapper<Object> {
	
	public Customer mapRow(ResultSet rs, int rowNum) throws SQLException
	{
		Customer customer = new Customer(
				rs.getInt(1),
				rs.getString(2),
				rs.getString(3),
				rs.getString(4),
				rs.getString(5),
				rs.getString(6),
				rs.getString(7)
				);
		
		return customer; 
		
	}
	

}