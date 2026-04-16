package jabaclass.admin.common.config;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
	basePackages = "jabaclass.admin.user.infrastructure.persistence",
	entityManagerFactoryRef = "userEntityManagerFactory",
	transactionManagerRef = "userTransactionManager"
)
public class UserDataSourceConfig {

	@Value("${jpa.ddl-auto:none}")
	private String ddlAuto;

	@Primary
	@Bean(name = "userDataSource")
	@ConfigurationProperties(prefix = "datasource.user")
	public DataSource userDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Primary
	@Bean(name = "userEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean userEntityManagerFactory(
		@Qualifier("userDataSource") DataSource dataSource
	) {
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setDataSource(dataSource);
		factory.setPackagesToScan("jabaclass.admin.user.domain.model");
		factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factory.setJpaPropertyMap(Map.of(
			"hibernate.hbm2ddl.auto", ddlAuto,
			"hibernate.physical_naming_strategy",
			"org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
		));
		return factory;
	}

	@Primary
	@Bean(name = "userTransactionManager")
	public PlatformTransactionManager userTransactionManager(
		@Qualifier("userEntityManagerFactory") LocalContainerEntityManagerFactoryBean factory
	) {
		return new JpaTransactionManager(factory.getObject());
	}
}
