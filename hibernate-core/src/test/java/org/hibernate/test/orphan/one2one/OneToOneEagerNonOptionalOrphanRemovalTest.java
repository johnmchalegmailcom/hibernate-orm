/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan.one2one;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Chris Cranford
 */
@TestForIssue( jiraKey = "HHH-9663" )
public class OneToOneEagerNonOptionalOrphanRemovalTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Car.class, PaintColor.class, Engine.class };
	}

	@Test
	public void testOneToOneLazyNonOptionalOrphanRemoval() {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Initialize the data
		doInHibernate( this::sessionFactory, session -> {
			final PaintColor color = new PaintColor( 1, "Red" );
			final Engine engine1 = new Engine( 1, 275 );
			final Engine engine2 = new Engine( 2, 295 );
			final Car car = new Car( 1, engine1, color );

			session.save( engine1 );
			session.save( engine2 );
			session.save( color );
			session.save( car );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Test orphan removal for unidirectional relationship
		doInHibernate( this::sessionFactory, session -> {
			final Car car = session.find( Car.class, 1 );
			final Engine engine = session.find( Engine.class, 2 );
			car.setEngine( engine );
			session.update( car );
		} );

		doInHibernate( this::sessionFactory, session -> {
			final Car car = session.find( Car.class, 1 );
			assertNotNull( car.getEngine() );

			final Engine engine = session.find( Engine.class, 1 );
			assertNull( engine );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Test orphan removal for bidirectional relationship
		doInHibernate( this::sessionFactory, session -> {
			final PaintColor color = new PaintColor( 2, "Blue" );
			final Car car = session.find( Car.class, 1 );
			car.setPaintColor( color );
			session.save( color );
			session.update( car );
		} );

		doInHibernate( this::sessionFactory, session -> {
			final Car car = session.find( Car.class, 1 );
			assertNotNull( car.getPaintColor() );

			final PaintColor color = session.find( PaintColor.class, 1 );
			assertNull( color );
		} );
	}

	@Entity(name = "Car")
	public static class Car {
		@Id
		private Integer id;

		// represents a bidirectional one-to-one
		@OneToOne(orphanRemoval = true, optional = false)
		private PaintColor paintColor;

		// represents a unidirectional one-to-one
		@OneToOne(orphanRemoval = true, optional = false)
		private Engine engine;

		Car() {
			// Required by JPA
		}

		Car(Integer id, Engine engine, PaintColor paintColor) {
			this.id = id;
			this.engine = engine;
			this.paintColor = paintColor;
			paintColor.setCar( this );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public PaintColor getPaintColor() {
			return paintColor;
		}

		public void setPaintColor(PaintColor paintColor) {
			this.paintColor = paintColor;
		}

		public Engine getEngine() {
			return engine;
		}

		public void setEngine(Engine engine) {
			this.engine = engine;
		}
	}

	@Entity(name = "Engine")
	public static class Engine {
		@Id
		private Integer id;
		private Integer horsePower;

		Engine() {
			// Required by JPA
		}

		Engine(Integer id, int horsePower) {
			this.id = id;
			this.horsePower = horsePower;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getHorsePower() {
			return horsePower;
		}

		public void setHorsePower(Integer horsePower) {
			this.horsePower = horsePower;
		}
	}

	@Entity(name = "PaintColor")
	public static class PaintColor {
		@Id
		private Integer id;
		private String color;

		@OneToOne(mappedBy = "paintColor")
		private Car car;

		PaintColor() {
			// Required by JPA
		}

		PaintColor(Integer id, String color) {
			this.id = id;
			this.color = color;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}

		public Car getCar() {
			return car;
		}

		public void setCar(Car car) {
			this.car = car;
		}
	}
}
