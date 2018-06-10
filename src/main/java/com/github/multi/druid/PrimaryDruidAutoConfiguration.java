package com.github.multi.druid;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;

/**
 * 多数据库默认的第一个数据源
 * 
 * @author wangzhifeng
 * @date 2018年5月25日 下午6:22:56
 */
@Configuration
@EnableConfigurationProperties({ PrimaryDruidDataSourceProperties.class })
@MapperScan(value = { "com.github.multi.demo.dal.dao" }, sqlSessionFactoryRef = "primarySqlSessionFactory")
public class PrimaryDruidAutoConfiguration {

    /**
     * one 对应配置文件的数据库名称
     */
    static final String                      MAPPER_LOCATION = "classpath*:sqlmap/one/*Mapper.xml";

    @Autowired
    private PrimaryDruidDataSourceProperties primaryDruidDataSourceProperties;

    @Bean(name = "primaryDruidDataSource", initMethod = "init", destroyMethod = "close")
    @ConditionalOnMissingBean(name = "primaryDruidDataSource")
    @Primary //第一个默认的数据源
    public DruidDataSource primaryDruidDataSource() throws Exception {
        DruidDataSource result = new DruidDataSource();
        result.setName(primaryDruidDataSourceProperties.getName());
        result.setUrl(primaryDruidDataSourceProperties.getUrl());
        result.setUsername(primaryDruidDataSourceProperties.getUsername());
        result.setPassword(primaryDruidDataSourceProperties.getPassword());
        result.setConnectionProperties(
                "config.decrypt=true;config.decrypt.key=" + primaryDruidDataSourceProperties.getPwdPublicKey());
        result.setFilters("config");
        result.setMaxActive(primaryDruidDataSourceProperties.getMaxActive());
        result.setInitialSize(primaryDruidDataSourceProperties.getInitialSize());
        result.setMaxWait(primaryDruidDataSourceProperties.getMaxWait());
        result.setMinIdle(primaryDruidDataSourceProperties.getMinIdle());
        result.setTimeBetweenEvictionRunsMillis(primaryDruidDataSourceProperties.getTimeBetweenEvictionRunsMillis());
        result.setMinEvictableIdleTimeMillis(primaryDruidDataSourceProperties.getMinEvictableIdleTimeMillis());
        result.setValidationQuery(primaryDruidDataSourceProperties.getValidationQuery());
        result.setTestWhileIdle(primaryDruidDataSourceProperties.isTestWhileIdle());
        result.setTestOnBorrow(primaryDruidDataSourceProperties.isTestOnBorrow());
        result.setTestOnReturn(primaryDruidDataSourceProperties.isTestOnReturn());
        result.setPoolPreparedStatements(primaryDruidDataSourceProperties.isPoolPreparedStatements());
        result.setMaxOpenPreparedStatements(primaryDruidDataSourceProperties.getMaxOpenPreparedStatements());

        if (primaryDruidDataSourceProperties.isEnableMonitor()) {
            StatFilter filter = new StatFilter();
            filter.setLogSlowSql(primaryDruidDataSourceProperties.isLogSlowSql());
            filter.setMergeSql(primaryDruidDataSourceProperties.isMergeSql());
            filter.setSlowSqlMillis(primaryDruidDataSourceProperties.getSlowSqlMillis());
            List<Filter> list = new ArrayList<Filter>();
            list.add(filter);
            result.setProxyFilters(list);
        }
        return result;
    }

    @Bean(name = "primaryTransactionManager")
    @ConditionalOnMissingBean(name = "primaryTransactionManager")
    @Primary
    public DataSourceTransactionManager transactionManager(@Qualifier("primaryDruidDataSource") DruidDataSource druidDataSource) {
        return new DataSourceTransactionManager(druidDataSource);
    }

    @Bean(name = "primaryTransactionTemplate")
    @ConditionalOnMissingBean(name = "primaryTransactionTemplate")
    @Primary
    public TransactionTemplate transactionTemplate(@Qualifier("primaryTransactionManager") PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }

    @Bean(name = "primarySqlSessionFactory")
    @ConditionalOnMissingBean(name = "primarySqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("primaryDruidDataSource") DruidDataSource druidDataSource)
            throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(druidDataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION));
        SqlSessionFactory sqlSessionFactory = sessionFactory.getObject();
        sqlSessionFactory.getConfiguration().setMapUnderscoreToCamelCase(true);
        return sqlSessionFactory;

    }
}
