package io.github.xuse.romking;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;

import org.junit.Test;

import com.github.xuse.querydsl.sql.expression.BeanCodec;
import com.github.xuse.querydsl.sql.expression.BeanCodecManager;

public class CodecRecordTest {
	
	@Test
	public void testRecord() {
		BeanCodec codec = BeanCodecManager.getInstance().getCodec(User.class);

		User foo = new User("ZHangsan","user@host.com");
		
		Object[] values = codec.values(foo);
		System.out.println(Arrays.toString(values));
		{
			User foo2 = (User) codec.newInstance(values);
			assertEquals(foo, foo2);
		}
		{
			// Test Copy
			User foo2 = new User("","");
			//codec.copy(foo, foo2);
			assertNotEquals(foo, foo2);
			// Test set
			User foo3 = new User("","");
			//codec.sets(values, foo3);
			assertNotEquals(foo, foo3);
		}
		values[1] = "LLL";
		{
			User foo2 = (User) codec.newInstance(values);
			System.out.println(foo2);
			assertNotEquals(foo, foo2);
			
			User foo3=new User("","");
			//codec.sets(values, foo3);
			assertNotEquals(foo2, foo3);
		}
	}
	

}
