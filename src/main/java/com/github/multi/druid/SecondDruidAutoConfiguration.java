package com.github.multi.druid;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;

/**
 * 多数据库默认的第二个数据源
 * 
 * @author wangzhifeng
 * @date 2018年5月25日 下午6:22:56
 */
@Configuration
@EnableConfigurationProperties({ SecondDruidDataSourceProperties.class })
@MapperScan(value = { "com.github.multi.demo.dal.dao" }, sqlSessionFactoryRef = "secondSqlSessionFactory")
@ConditionalOnProperty(name = "spring.druid.datasource.two.url", matchIfMissing = false)
public class SecondDruidAutoConfiguration {

    /**
     * one 对应配置文件的数据库名称
     */
    static final String                     MAPPER_LOCATION = "classpath*:sqlmap/one/*Mapper.xml";

    @Autowired
    private SecondDruidDataSourceProperties secondDruidDataSourceProperties;

    @Bean(name = "secondDruidDataSource", initMethod = "init", destroyMethod = "close")
    @ConditionalOnMissingBean(name = "secondDruidDataSource")
    public DruidDataSource secondDruidDataSource() throws Exception {
        DruidDataSource result = new DruidDataSource();
        result.setName(secondDruidDataSourceProperties.getName());
        result.setUrl(secondDruidDataSourceProperties.getUrl());
        result.setUsername(secondDruidDataSourceProperties.getUsername());
        result.setPassword(secondDruidDataSourceProperties.getPassword());
        result.setConnectionProperties(
                "config.decrypt=true;config.decrypt.key=" + secondDruidDataSourceProperties.getPwdPublicKey());
        result.setFilters("config");
        result.setMaxActive(secondDruidDataSourceProperties.getMaxActive());
        result.setInitialSize(secondDruidDataSourceProperties.getInitialSize());
        result.setMaxWait(secondDruidDataSourceProperties.getMaxWait());
        result.setMinIdle(secondDruidDataSourceProperties.getMinIdle());
        result.setTimeBetweenEvictionRunsMillis(secondDruidDataSourceProperties.getTimeBetweenEvictionRunsMillis());
        result.setMinEvictableIdleTimeMillis(secondDruidDataSourceProperties.getMinEvictableIdleTimeMillis());
        result.setValidationQuery(secondDruidDataSourceProperties.getValidationQuery());
        result.setTestWhileIdle(secondDruidDataSourceProperties.isTestWhileIdle());
        result.setTestOnBorrow(secondDruidDataSourceProperties.isTestOnBorrow());
        result.setTestOnReturn(secondDruidDataSourceProperties.isTestOnReturn());
        result.setPoolPreparedStatements(secondDruidDataSourceProperties.isPoolPreparedStatements());
        result.setMaxOpenPreparedStatements(secondDruidDataSourceProperties.getMaxOpenPreparedStatements());

        if (secondDruidDataSourceProperties.isEnableMonitor()) {
            StatFilter filter = new StatFilter();
            filter.setLogSlowSql(secondDruidDataSourceProperties.isLogSlowSql());
            filter.setMergeSql(secondDruidDataSourceProperties.isMergeSql());
            filter.setSlowSqlMillis(secondDruidDataSourceProperties.getSlowSqlMillis());
            List<Filter> list = new ArrayList<Filter>();
            list.add(filter);
            result.setProxyFilters(list);
        }
        return result;
    }

    @Bean(name = "secondTransactionManager")
    @ConditionalOnMissingBean(name = "secondTransactionManager")
    public DataSourceTransactionManager transactionManager(@Qualifier("secondDruidDataSource") DruidDataSource druidDataSource) {
        return new DataSourceTransactionManager(druidDataSource);
    }

    @Bean(name = "secondTransactionTemplate")
    @ConditionalOnMissingBean(name = "secondTransactionTemplate")
    public TransactionTemplate transactionTemplate(@Qualifier("secondTransactionManager") PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }

    @Bean(name = "secondSqlSessionFactory")
    @ConditionalOnMissingBean(name = "secondSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("secondDruidDataSource") DruidDataSource druidDataSource)
            throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(druidDataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION));
        SqlSessionFactory sqlSessionFactory = sessionFactory.getObject();
        sqlSessionFactory.getConfiguration().setMapUnderscoreToCamelCase(true);
        return sqlSessionFactory;

    }
}
