import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Light and fast',
    icon: <img src="img/deadline.png" width="60px" />,
    description: (
      <>
        DC Comic includes a very light IoC framework, build time oriented.
      </>
    ),
  },
  {
    title: 'Cloud & GraalVM',
    icon: <img src="img/cloud.png" width="60px" />,
    description: (
      <>
        DC Comic is cloud native, with seamless integration with Kubernetes. It also support GraalVM natively to bootstrap efficienly.
      </>
    ),
  },
  {
    title: 'Turnkey extensions',
    icon: <img src="img/deal.png" width="60px" />,
    description: (
      <>
        DC Comic brings turnkey extensions you can leverage at no cost in your cloud applications: structured logging, telemetry, gRPC, ...
      </>
    ),
  },
];

function Feature({icon, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        {icon}
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
